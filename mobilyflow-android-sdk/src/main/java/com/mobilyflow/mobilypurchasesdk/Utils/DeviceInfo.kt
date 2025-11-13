package com.mobilyflow.mobilypurchasesdk.Utils

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.appset.AppSet
import java.util.UUID


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

        fun getInstallIdentifier(context: Context): String {
            val prefs = context.getSharedPreferences("com.mobilyflow", Context.MODE_PRIVATE)

            var installIdentifier = prefs.getString("com.mobilyflow.installIdentifier", null)

            if (installIdentifier == null) {
                installIdentifier = UUID.randomUUID().toString().lowercase()
                with(prefs.edit()) {
                    putString("com.mobilyflow.installIdentifier", installIdentifier)
                    apply()
                }
            }

            return installIdentifier
        }

        fun getIdfv(context: Context): String? {
            val client = AppSet.getClient(context)
            val result = AsyncResult<String?>()

            client.appSetIdInfo.addOnSuccessListener {
                result.set(it.id)
            }.addOnFailureListener {
                result.set(null)
            }.addOnCanceledListener {
                result.set(null)
            }

            result.waitResult()
            return result.dataOptional()
        }

        fun getAdid(context: Context): String? {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
            if (info.isLimitAdTrackingEnabled) {
                return null
            } else {
                val id = info.id

                if (id == null || id == "00000000-0000-0000-0000-000000000000") {
                    return null
                }

                return id
            }
        }
    }
}