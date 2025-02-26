package com.mobilyflow.mobilypurchasesdk.SDKHelpers

import android.content.pm.PackageInfo
import android.os.Build
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.Monitoring.Monitoring
import java.util.concurrent.Executors


class MobilyPurchaseSDKDiagnostics(val billingClient: BillingClientWrapper, var customerId: String?) {
    fun sendDiagnostic() {
        Executors.newSingleThreadExecutor().execute {
            // 1. Write maximum info we can get
            runCatching {
                val packageName = billingClient.context.packageName
                val pInfo: PackageInfo = billingClient.context.packageManager.getPackageInfo(packageName, 0)
                val versionName = pInfo.versionName
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else pInfo.versionCode
                
                Logger.d("App $packageName version $versionName ($versionCode)")
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
                Monitoring.exportDiagnostic(1)
            }
        }
    }
}