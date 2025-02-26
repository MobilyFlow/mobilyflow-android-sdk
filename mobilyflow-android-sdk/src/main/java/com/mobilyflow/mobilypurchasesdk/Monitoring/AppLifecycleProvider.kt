package com.mobilyflow.mobilypurchasesdk.Monitoring

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * This class is designed to provide static access to Application LifeCycle.
 * It should be added into `<provider>` tag on `AndroidManifest.xml`, nested into the `<application>` tag.
 */
class AppLifecycleProvider : ContentProvider(), ActivityLifecycleCallbacks {
    companion object {
        private val listeners = mutableListOf<AppLifecycleCallbacks>()

        fun registerListener(listener: AppLifecycleCallbacks) {
            listeners.add(listener)
        }

        fun unregisterListener(listener: AppLifecycleCallbacks) {
            listeners.remove(listener)
        }
    }

    open class AppLifecycleCallbacks {
        open fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        open fun onActivityStarted(activity: Activity) {}
        open fun onActivityResumed(activity: Activity) {}
        open fun onActivityPaused(activity: Activity) {}
        open fun onActivityStopped(activity: Activity) {}
        open fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        open fun onActivityDestroyed(activity: Activity) {}
        open fun onLowMemory() {}
        open fun onTrimMemory(level: Int) {}
        open fun uncaughtException(t: Thread?, e: Throwable?) {}
    }

    override fun onCreate(): Boolean {
        if (this.context!! is Application) {
            (this.context as Application).registerActivityLifecycleCallbacks(this)

            val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                listeners.forEach { listener ->
                    listener.uncaughtException(thread, throwable)
                }
                existingHandler?.uncaughtException(thread, throwable)
            }

            return true
        } else {
            return false
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        listeners.forEach { listener ->
            listener.onActivityCreated(activity, savedInstanceState)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        listeners.forEach { listener ->
            listener.onActivityStarted(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        listeners.forEach { listener ->
            listener.onActivityResumed(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        listeners.forEach { listener ->
            listener.onActivityPaused(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        listeners.forEach { listener ->
            listener.onActivityStopped(activity)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        listeners.forEach { listener ->
            listener.onActivitySaveInstanceState(activity, outState)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        listeners.forEach { listener ->
            listener.onActivityDestroyed(activity)
        }
    }

    override fun onLowMemory() {
        listeners.forEach { listener ->
            listener.onLowMemory()
        }
        super.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        listeners.forEach { listener ->
            listener.onTrimMemory(level)
        }
        super.onTrimMemory(level)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }
}