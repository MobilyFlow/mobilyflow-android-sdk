package com.mobilyflow.mobilypurchasesdk.SDKHelpers

import com.android.billingclient.api.Purchase
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyTransferOwnershipStatus
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyWebhookStatus
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyPurchaseException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyTransferOwnershipException
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MobilyPurchaseAPI
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.Utils.Utils

class MobilyPurchaseSDKWaiter(val API: MobilyPurchaseAPI, val diagnostics: MobilyPurchaseSDKDiagnostics) {
    @Throws(MobilyPurchaseException::class)
    fun waitPurchaseWebhook(purchase: Purchase): MobilyWebhookStatus {
        val startTime = System.currentTimeMillis()

        if (purchase.purchaseTime < (startTime - 7 * 24 * 3600 * 1000)) {
            // In case of a PURCHASE older than 1 week, assume the webhook is already done.
            Logger.w("finishTransaction with old purchaseDate -> skip waitWebhook")
            return MobilyWebhookStatus.SUCCESS
        }

        var result = MobilyWebhookStatus.PENDING
        var retry = 0

        Logger.d("Wait webhook for ${purchase.orderId} (purchaseToken: ${purchase.purchaseToken})")

        while (result == MobilyWebhookStatus.PENDING) {
            result = this.API.getWebhookStatus(purchase.purchaseToken, purchase.orderId!!)

            if (result == MobilyWebhookStatus.PENDING) {
                // Exit the wait function after 1 minute
                if (startTime + 60000 < System.currentTimeMillis()) {
                    Logger.e("Webhook still pending after 1 minutes (The user has probably paid without being credited)")
                    diagnostics.sendDiagnostic()
                    throw MobilyPurchaseException(MobilyPurchaseException.Type.WEBHOOK_NOT_PROCESSED)
                }

                Thread.sleep(Utils.calcWaitWebhookTime(retry))
                retry++
            }
        }

        Logger.d("Webhook wait completed (${result})")

        if (result == MobilyWebhookStatus.ERROR) {
            throw MobilyPurchaseException(MobilyPurchaseException.Type.WEBHOOK_FAILED)
        }

        return result
    }

    @Throws(MobilyTransferOwnershipException::class)
    fun waitTransferOwnershipWebhook(requestId: String): MobilyTransferOwnershipStatus {
        var result = MobilyTransferOwnershipStatus.PENDING
        val startTime = System.currentTimeMillis()
        var retry = 0

        while (result == MobilyTransferOwnershipStatus.PENDING) {
            result = this.API.getTransferRequestStatus(requestId)

            if (result == MobilyTransferOwnershipStatus.PENDING) {
                // Exit the wait function after 1 minute
                if (startTime + 60000 < System.currentTimeMillis()) {
                    throw MobilyTransferOwnershipException(MobilyTransferOwnershipException.Type.WEBHOOK_NOT_PROCESSED)
                }

                Thread.sleep(Utils.calcWaitWebhookTime(retry))
                retry++
            }
        }

        return result
    }
}