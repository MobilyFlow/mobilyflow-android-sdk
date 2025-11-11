package com.mobilyflow.mobilypurchasesdk.SDKHelpers

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.Monitoring.Monitoring
import com.mobilyflow.mobilypurchasesdk.Utils.DeviceInfo
import java.util.concurrent.Executors


class MobilyPurchaseSDKDiagnostics(
    val context: Context,
    val billingClient: BillingClientWrapper,
    var customerId: String?
) {
    fun sendDiagnostic(sinceDays: Int = 1) {
        Executors.newSingleThreadExecutor().execute {
            // 1. Write maximum info we can get
            runCatching {
                Logger.d("[Device Info] OS = Android ${DeviceInfo.getOSVersion()}")
                Logger.d("[Device Info] deviceModel = ${DeviceInfo.getDeviceModelName()}")
                Logger.d("[Device Info] appPackage = ${DeviceInfo.getAppPackage(context)}")
                Logger.d(
                    "[Device Info] appVersion = ${DeviceInfo.getAppVersionName(context)} (${
                        DeviceInfo.getAppVersionCode(context)
                    })"
                )
            }
            runCatching {
                if (customerId == null) {
                    Logger.w("Not logged to a customer...")
                } else {
                    Logger.d("Logged with customer $customerId")
                }
            }
            runCatching {
                val purchases = billingClient.queryPurchases()
                purchases.forEach {
                    Logger.d("Purchase (${it.type}): products: ${it.purchase.products}, purchaseToken: ${it.purchase.purchaseToken}")
                }
            }

            // 2. Send diagnostics
            runCatching {
                Monitoring.exportDiagnostic(sinceDays)
            }
        }
    }
}