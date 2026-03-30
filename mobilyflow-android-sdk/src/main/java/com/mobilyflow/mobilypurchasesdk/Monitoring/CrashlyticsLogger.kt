package com.mobilyflow.mobilypurchasesdk.Monitoring

import android.util.Log

/// Utility class to log messages to Firebase Crashlytics if available in the host app.
/// This uses reflection to dynamically call Crashlytics methods without
/// requiring Firebase as a dependency.
class CrashlyticsLogger {
    companion object {
        private var crashlyticsInstance: Any? = null
        private var isInitialized = false
        private var isAvailable = false

        private var logMethod: java.lang.reflect.Method? = null

        /// Initialize and cache the Crashlytics instance if available
        @Synchronized
        private fun initialize() {
            if (isInitialized) return
            isInitialized = true

            try {
                val clazz = Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
                val getInstanceMethod = clazz.getMethod("getInstance")
                crashlyticsInstance = getInstanceMethod.invoke(null)

                if (crashlyticsInstance != null) {
                    logMethod = clazz.getMethod("log", String::class.java)
                    isAvailable = true
                    Log.d("MobilyFlow", "Firebase Crashlytics detected")
                }
            } catch (e: Exception) {
                isAvailable = false
            }
        }

        /// Check if Firebase Crashlytics is available in the host app
        fun isCrashlyticsAvailable(): Boolean {
            initialize()
            return isAvailable
        }

        /// Log a message to Firebase Crashlytics.
        /// Messages will appear in the "Logs" tab of a crash report.
        fun log(message: String) {
            initialize()
            val instance = crashlyticsInstance ?: return

            try {
                logMethod?.invoke(instance, message)
            } catch (e: Exception) {
                // no-op
            }
        }
    }
}
