package com.mobilyflow.mobilypurchasesdk.SDKHelpers

import com.mobilyflow.mobilypurchasesdk.Enums.TransferOwnershipStatus
import com.mobilyflow.mobilypurchasesdk.Enums.WebhookStatus
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyPurchaseException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyTransferOwnershipException
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MobilyPurchaseAPI
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.Utils.Utils

class MobilyPurchaseSDKWaiter(val API: MobilyPurchaseAPI, val diagnostics: MobilyPurchaseSDKDiagnostics) {
    @Throws(MobilyPurchaseException::class)
    fun waitPurchaseWebhook(transactionId: String, isDowngrade: Boolean): WebhookStatus {
        var result = WebhookStatus.PENDING
        val startTime = System.currentTimeMillis()
        var retry = 0

        while (result == WebhookStatus.PENDING) {
            result = this.API.getWebhookStatus(transactionId, isDowngrade)

            if (result == WebhookStatus.PENDING) {
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

        if (result == WebhookStatus.ERROR) {
            throw MobilyPurchaseException(MobilyPurchaseException.Type.WEBHOOK_FAILED)
        }

        return result
    }

    @Throws(MobilyTransferOwnershipException::class)
    fun waitTransferOwnershipWebhook(requestId: String): TransferOwnershipStatus {
        var result = TransferOwnershipStatus.PENDING
        val startTime = System.currentTimeMillis()
        var retry = 0

        while (result == TransferOwnershipStatus.PENDING) {
            result = this.API.getTransferRequestStatus(requestId)

            if (result == TransferOwnershipStatus.PENDING) {
                // Exit the wait function after 1 minute
                if (startTime + 60000 < System.currentTimeMillis()) {
                    throw MobilyTransferOwnershipException(MobilyTransferOwnershipException.Type.WEBHOOK_NOT_PROCESSED)
                }

                Thread.sleep(Utils.calcWaitWebhookTime(retry))
                retry++
            }
        }

        if (result == TransferOwnershipStatus.ERROR) {
            throw MobilyTransferOwnershipException(MobilyTransferOwnershipException.Type.WEBHOOK_FAILED)
        }

        return result
    }
}