package com.mobilyflow.mobilypurchasesdk.Monitoring

import android.app.Activity
import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

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
        private fun exportLogs(
            fromDate: LocalDate? = null,
            toDate: LocalDate? = null,
            clearLogs: Boolean = false
        ): File {
            checkInit()

            var from = fromDate ?: LocalDate.now()
            val to = toDate ?: LocalDate.now()

            val buffer = ByteArray(8192)
            var bytesRead: Int

            val exportFolder = File(baseDir, "mobilyflow/exported-logs")
            if (!exportFolder.exists()) {
                exportFolder.mkdirs()
            }
            val targetFile = File(exportFolder, "${System.currentTimeMillis()}.log")

            FileOutputStream(targetFile, false).use { fos ->
                BufferedOutputStream(fos).use { writer ->
                    while (!from.isAfter(to)) { // Is before or equal

                        val logFile = File(baseDir, "mobilyflow/logs/" + Logger.getLogFileName(slug!!, from))
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
                            if (lastWritingDate != null && from.isEqual(lastWritingDate)) {
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
                        from = from.plusDays(1)
                    }
                }
            }

            return targetFile
        }

        /**
         * Export logs from sinceDays to now().
         *
         * If sinceDays is 0, export only today, if it 1 export also yesterday, etc...
         * If clearLogs is true, remove exported logfiles
         */
        private fun exportLogs(sinceDays: Int = 0, clearLogs: Boolean = false): File {
            val fromDate = LocalDate.now().minusDays(sinceDays.toLong())
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
                val exportFolder = File(context.filesDir, "mobilyflow/exported-logs")
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