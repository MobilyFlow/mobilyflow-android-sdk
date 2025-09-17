package com.mobilyflow.mobilypurchasesdk.Utils

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build


class DeviceInfo {
    companion object {
        private fun getPackageInfo(context: Context): PackageInfo {
            return context.packageManager.getPackageInfo(context.packageName, 0)
        }

        fun getOSVersion(): String {
            return Build.VERSION.RELEASE
        }

        fun getDeviceModelName(): String {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL

            val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) {
                model
            } else {
                "$manufacturer $model"
            }

            return deviceName
        }

        fun getAppPackage(context: Context): String {
            return getPackageInfo(context).packageName
        }

        fun getAppVersionName(context: Context): String {
            return getPackageInfo(context).versionName ?: "null"
        }

        fun getAppVersionCode(context: Context): Int {
            return getPackageInfo(context).versionCode
        }
    }
}