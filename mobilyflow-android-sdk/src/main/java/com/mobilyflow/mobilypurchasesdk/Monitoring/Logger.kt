package com.mobilyflow.mobilypurchasesdk.Monitoring

import android.app.Activity
import android.util.Log
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Logger private constructor(internal val baseDir: File, private val tag: String, allowLogging: Boolean) :
    Closeable {

    private val allowLogging = allowLogging
    private var slug: String

    internal var lastWritingDate: LocalDate? = null
    internal var stream: BufferedOutputStream? = null

    private var lifecycleListener: AppLifecycleProvider.AppLifecycleCallbacks
    private var flushTask: ScheduledFuture<*>? = null

    init {
        slug = slugify(tag)
        ensureFileRotation()

        lifecycleListener = object : AppLifecycleProvider.AppLifecycleCallbacks() {
            override fun onActivityResumed(activity: Activity) {
                startFlushTask()
            }

            override fun onActivityPaused(activity: Activity) {
                stopFlushTask()
            }

            override fun onLowMemory() {
                flush()
            }

            override fun onTrimMemory(level: Int) {
                flush()
            }

            override fun uncaughtException(t: Thread?, e: Throwable?) {
                e("UncaughtExceptionHandler", e)
                flush()
            }
        }
        AppLifecycleProvider.registerListener(lifecycleListener)
        startFlushTask()
    }

    override fun close() {
        AppLifecycleProvider.unregisterListener(lifecycleListener)
        stopFlushTask()
    }

    private fun startFlushTask() {
        stopFlushTask()

        flushTask = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            this.flush()
        }, 0, 10, TimeUnit.SECONDS)
    }

    private fun stopFlushTask() {
        flush()
        flushTask?.cancel(false)
        flushTask = null
    }

    private fun getLevelLabel(level: Int): String {
        return when (level) {
            Log.VERBOSE -> "VERSBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "UNKNOWN"
        }
    }

    /**
     * This function is responsible to:
     *  1. Close the stream & create a new one if we are on a new day
     *  2. Remove old log file, keeping only files from last 5 days
     */
    private fun ensureFileRotation() {
        val nowDate = LocalDate.now()
        if (stream == null || nowDate.isAfter(lastWritingDate)) {
            // Need to rotate

            val logFolder = File(baseDir, "mobilyflow/logs")
            if (!logFolder.exists()) {
                logFolder.mkdirs()
            }

            synchronized(this) {
                // 1. Close old stream
                if (stream != null) {
                    runCatching {
                        stream!!.close()
                    }
                }

                // 2. Create new stream
                val file = File(logFolder, getLogFileName(slug))
                stream = BufferedOutputStream(FileOutputStream(file, true))
                lastWritingDate = nowDate
            }

            // 3. Delete old files in a new thread
            Executors.newSingleThreadExecutor().execute {
                val allLogFiles = logFolder.listFiles { file ->
                    file.isFile && file.name.startsWith(slug + "_") && file.name.endsWith(".log")
                }?.sortedBy { file -> file.name }

                if (!allLogFiles.isNullOrEmpty()) {
                    val regex = "^${slug}_(.+)\\.log$".toPattern()

                    for (logFile in allLogFiles) {
                        kotlin.runCatching {
                            val matcher = regex.matcher(logFile.name)

                            if (matcher.find()) {
                                val fileDate = LocalDate.parse(matcher.group(1))
                                val daysBetween = fileDate.until(nowDate, ChronoUnit.DAYS)
                                if (daysBetween >= 5) {
                                    logFile.delete()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun flush() {
        synchronized(this) {
            runCatching {
                stream!!.flush()
            }
        }
    }


    fun log(level: Int, msg: String, tr: Throwable? = null) {
        ensureFileRotation()

        var finalMsg = msg
        if (tr != null) {
            finalMsg += "\n" + Log.getStackTraceString(tr)
        }

        if (allowLogging) {
            Log.println(level, tag, finalMsg)
        }

        finalMsg = LocalDateTime.now()
            .format(DateTimeFormatter.ISO_DATE_TIME) + " [" + getLevelLabel(level) + "] " + finalMsg + '\n'
        synchronized(this) {
            runCatching {
                stream!!.write(finalMsg.toByteArray())
            }
        }
    }

    fun d(msg: String, tr: Throwable? = null) {
        log(Log.DEBUG, msg, tr)
    }

    fun w(msg: String, tr: Throwable? = null) {
        log(Log.WARN, msg, tr)
    }

    fun e(msg: String, tr: Throwable? = null) {
        log(Log.ERROR, msg, tr)
    }

    companion object {
        internal var logger: Logger? = null

        internal fun initialize(baseDir: File, tag: String, production: Boolean) {
            logger = Logger(baseDir, tag, production)
        }

        private fun checkInit() {
            if (logger == null) {
                throw Exception("Monitoring not initialized")
            }
        }

        fun d(msg: String, tr: Throwable? = null) {
            checkInit()
            logger?.d(msg, tr)
        }

        fun w(msg: String, tr: Throwable? = null) {
            checkInit()
            logger?.w(msg, tr)
        }

        fun e(msg: String, tr: Throwable? = null) {
            checkInit()
            logger?.e(msg, tr)
        }

        internal fun getLogFileName(slug: String, date: LocalDate? = null): String {
            return slug + "_" + (date ?: LocalDate.now()).format(DateTimeFormatter.ISO_DATE) + ".log"
        }

        internal fun slugify(input: String): String {
            // Normalize the string to remove accent & diacritics
            var slug = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "") // Remove diacritic marks

            // Remove special characters except underscores and space
            slug = slug.replace(Regex("[^a-zA-Z0-9_\\s]"), "")

            // Trim
            slug = slug.replace(Regex("\\s+"), " ").trim()

            // Replace space with dash
            slug = slug.replace(" ", "-")

            return slug
        }
    }
}