package com.mobilyflow.mobilypurchasesdk.Monitoring

import android.app.Activity
import android.content.Context
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

abstract class Monitoring {
    companion object {
        private var sendHandler: ((file: File) -> Unit)? = null
        private var sendTask: ScheduledFuture<*>? = null
        private var lifecycleListener: AppLifecycleProvider.AppLifecycleCallbacks? = null

        private var baseDir: File? = null
        private var slug: String? = null

        fun initialize(context: Context, tag: String, allowLogging: Boolean, sendHandler: ((file: File) -> Unit)) {
            this.sendHandler = sendHandler
            this.baseDir = context.filesDir
            this.slug = Logger.slugify(tag)
            Logger.initialize(this.baseDir!!, tag, allowLogging)

            // TODO: This should be removed and is here for retro-compatibility

            try {
                val baseLogFolder = Logger.getLogFolder(null)
                val rawLogFolder = Logger.getLogFolder(LogFolderType.RAW_LOGS)

                val listFiles = baseLogFolder.listFiles()
                if (listFiles != null) {
                    for (oldFile in listFiles) {
                        if (oldFile.isFile && oldFile.name.endsWith(".log")) {
                            val newFile = File(rawLogFolder, oldFile.name)

                            if (newFile.exists()) {
                                // New file already exists, remove the old one
                                Logger.d("Remove old log file ${oldFile.path}")
                                newFile.delete()
                            } else {
                                Logger.d("Move old log file ${oldFile.path} to ${newFile.path}")
                                Utils.moveFile(oldFile, newFile)
                            }
                        }
                    }
                }
            } catch (error: Exception) {
                Logger.e("Can't move log to new structure", error)
            }

            // ----------------------------------------------------------------

            lifecycleListener = object : AppLifecycleProvider.AppLifecycleCallbacks() {
                override fun onActivityResumed(activity: Activity) {
                    if (checkInit(false)) {
                        startSendTask(activity)
                    }
                }

                override fun onActivityPaused(activity: Activity) {
                    stopSendTask()
                }
            }
            AppLifecycleProvider.registerListener(lifecycleListener!!)
            startSendTask(context)
        }

        private fun checkInit(throwOnError: Boolean = true): Boolean {
            if (baseDir == null) {
                if (throwOnError) {
                    throw Exception("Monitoring not initialized")
                }
                return false
            }
            return true
        }

        fun close() {
            checkInit()
            AppLifecycleProvider.unregisterListener(lifecycleListener!!)
            Logger.logger!!.close()
        }


        /**
         * Export logs from fromDate to toDate (both included) into a new file.
         *
         * startDate and toDate both default to now() if they are null.
         * If clearLogs is true, remove exported logfiles
         */
        @OptIn(ExperimentalTime::class)
        private fun exportLogs(
            fromDate: LocalDate? = null,
            toDate: LocalDate? = null,
            clearLogs: Boolean = false
        ): File {
            checkInit()

            var from = fromDate ?: Clock.System.todayIn(TimeZone.UTC)
            val to = toDate ?: Clock.System.todayIn(TimeZone.UTC)

            val buffer = ByteArray(8192)
            var bytesRead: Int

            val rawLogFolder = Logger.getLogFolder(LogFolderType.RAW_LOGS)
            val processingLogFolder = Logger.getLogFolder(LogFolderType.PROCESSING_LOGS)
            val exportLogFolder = Logger.getLogFolder(LogFolderType.EXPORT_LOGS)

            val processingFile = File(processingLogFolder, "${System.currentTimeMillis()}.log")
            val exportFile = File(exportLogFolder, "${System.currentTimeMillis()}.log")

            FileOutputStream(processingFile, false).use { fos ->
                BufferedOutputStream(fos).use { writer ->
                    while (from.toEpochDays() <= to.toEpochDays()) { // Is before or equal

                        val logFile = File(rawLogFolder, Logger.getLogFileName(slug!!, from))
                        if (logFile.exists() && logFile.isFile) {
                            val copyAction = {
                                FileInputStream(logFile).use { fis ->
                                    BufferedInputStream(fis).use { reader ->
                                        while (reader.read(buffer).also { bytesRead = it } != -1) {
                                            // Write the data to the destination file
                                            writer.write(buffer, 0, bytesRead)
                                        }
                                    }
                                }
                            }

                            val lastWritingDate = Logger.logger!!.lastWritingDate
                            if (lastWritingDate != null && from.toEpochDays() == lastWritingDate.toEpochDays()) {
                                // In case we export the current logFile, we need to synchronize the read process
                                synchronized(Logger.logger!!) {
                                    if (clearLogs) {
                                        Logger.logger!!.stream!!.close()
                                    } else {
                                        Logger.logger!!.stream!!.flush()
                                    }

                                    copyAction()

                                    if (clearLogs) {
                                        logFile.delete()
                                        Logger.logger!!.stream = BufferedOutputStream(FileOutputStream(logFile, true))
                                    }
                                }
                            } else {
                                copyAction()

                                if (clearLogs) {
                                    logFile.delete()
                                }
                            }
                        }
                        from = from.plus(1, DateTimeUnit.DAY)
                    }
                }
            }

            // Move file from processing to export folder
            Utils.moveFile(processingFile, exportFile)

            return exportFile
        }

        /**
         * Export logs from sinceDays to now().
         *
         * If sinceDays is 0, export only today, if it 1 export also yesterday, etc...
         * If clearLogs is true, remove exported logfiles
         */
        @OptIn(ExperimentalTime::class)
        private fun exportLogs(sinceDays: Int = 0, clearLogs: Boolean = false): File {
            val fromDate = Clock.System.todayIn(TimeZone.UTC).minus(sinceDays.toLong(), DateTimeUnit.DAY)
            return exportLogs(fromDate, null, clearLogs)
        }

        fun exportDiagnostic(sinceDays: Int) {
            checkInit()

            Executors.newSingleThreadScheduledExecutor().execute {
                try {
                    val exportFile = exportLogs(sinceDays, true)
                    sendLogFile(exportFile)
                } catch (e: Exception) {
                    // Exception in sendHandler or exportLogs, retry sending logs next time
                    Logger.e("[exportDiagnostics] Error", e)
                }
            }
        }

        private fun sendLogFile(logFile: File) {
            if (!checkInit(false)) {
                return
            }

            try {
                this.sendHandler!!(logFile)
                logFile.delete() // No exception in sendHandler -> Remove file
            } catch (e: Exception) {
                // Exception in sendHandler or exportLogs, retry sending logs next time
                Logger.e("[exportDiagnostics] Error", e)
            }
        }

        private fun startSendTask(context: Context) {
            checkInit()

            stopSendTask()
            sendTask = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
                val exportFolder = Logger.getLogFolder(LogFolderType.EXPORT_LOGS)
                if (exportFolder.exists()) {
                    exportFolder.listFiles { file ->
                        file.isFile && file.name.endsWith(".log")
                    }?.forEach { logFile -> sendLogFile(logFile) }

                    if (exportFolder.list().isNullOrEmpty()) {
                        exportFolder.delete()
                    }
                }
            }, 2, 60, TimeUnit.SECONDS)
        }

        private fun stopSendTask() {
            sendTask?.cancel(false)
            sendTask = null
        }
    }
}