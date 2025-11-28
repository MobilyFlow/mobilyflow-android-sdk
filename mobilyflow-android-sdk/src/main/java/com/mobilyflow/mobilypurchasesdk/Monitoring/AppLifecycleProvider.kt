package com.mobilyflow.mobilypurchasesdk.Monitoring

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log

/**
 * This class is designed to provide static access to Application LifeCycle.
 * You must call `AppLifecycleProvider.init` as soon as possible.
 */
class AppLifecycleProvider {

    companion object {
        private val listeners = mutableListOf<AppLifecycleCallbacks>()
        private var instance: AppLifecycleProvider? = null

        fun init(application: Application) {
            if (instance == null) {
                instance = AppLifecycleProvider()
                application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
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
                })

                val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    listeners.forEach { listener ->
                        listener.uncaughtException(thread, throwable)
                    }
                    existingHandler?.uncaughtException(thread, throwable)
                }
            } else {
                Log.e("MobilyFlow", "AppLifecycleProvider already initialized")
            }
        }

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
        open fun uncaughtException(t: Thread?, e: Throwable?) {}
    }
}