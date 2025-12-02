package com.mobilyflow.test_android_sdk

import android.app.Application
import android.util.Log

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MobilyFlow", "Init App")
    }
}
