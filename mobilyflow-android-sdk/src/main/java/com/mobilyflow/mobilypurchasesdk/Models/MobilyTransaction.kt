package com.mobilyflow.mobilypurchasesdk.Models

import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyPlatform
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyStore
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyTransactionStatus
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import com.mobilyflow.mobilypurchasesdk.Utils.optStringNull
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject

class MobilyTransaction(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val storeId: String,
    val storeOriginalId: String?,
    val customerId: String,
    val quantity: Int,
    val country: String,
    val priceMillis: Int,
    val currency: String,
    val convertedPriceMillis: Int,
    val convertedCurrency: String,
    val status: MobilyTransactionStatus,
    val refundedPercent: Double,
    val productId: String,
    val subscriptionId: String?,
    val itemId: String?,
    val productOfferId: String?,
    val platform: MobilyPlatform,
    /**
     * Store where the purchase was made (App Store or Play Store), whereas `platform` is the OS of the device.
     * Null for purchases made before this field existed, until the backend backfill has run.
     */
    val store: MobilyStore?,
    val startsAt: LocalDateTime?,
    val endsAt: LocalDateTime?,
    val refundedAt: LocalDateTime?,
    val isSandbox: Boolean,
) {
    companion object {
        internal fun parse(
            jsonTx: JSONObject,
            storeAccountTransactions: List<BillingClientWrapper.PurchaseWithType>?
        ): MobilyTransaction {
            val platform = MobilyPlatform.parse(jsonTx.getString("platform"))
            var storeOriginalId: String? = jsonTx.getString("storeOriginalId")

            if (platform == MobilyPlatform.ANDROID) {
                val relatedPurchase =
                    Utils.getPurchaseWithSha256PurchaseToken(storeOriginalId, storeAccountTransactions)
                val storeAccountTx = relatedPurchase?.purchase
                storeOriginalId = storeAccountTx?.purchaseToken
            }

            return MobilyTransaction(
                id = jsonTx.getString("id"),
                createdAt = Utils.parseDate(jsonTx.getString("createdAt")),
                updatedAt = Utils.parseDate(jsonTx.getString("updatedAt")),
                storeId = jsonTx.getString("storeId"),
                storeOriginalId = storeOriginalId,
                customerId = jsonTx.getString("customerId"),
                quantity = jsonTx.optInt("quantity", 1),
                country = jsonTx.getString("country"),
                priceMillis = jsonTx.getInt("priceMillis"),
                currency = jsonTx.getString("currency"),
                convertedPriceMillis = jsonTx.getInt("convertedPriceMillis"),
                convertedCurrency = jsonTx.getString("convertedCurrency"),
                status = MobilyTransactionStatus.parse(jsonTx.getString("status")),
                refundedPercent = jsonTx.optDouble("refundedPercent", 0.0),
                productId = jsonTx.getString("productId"),
                subscriptionId = jsonTx.optStringNull("subscriptionId"),
                itemId = jsonTx.optStringNull("itemId"),
                productOfferId = jsonTx.optStringNull("productOfferId"),
                platform = platform,
                store = MobilyStore.parse(jsonTx.optStringNull("store")),
                startsAt = Utils.parseDateOpt(jsonTx.optStringNull("startsAt")),
                endsAt = Utils.parseDateOpt(jsonTx.optStringNull("endsAt")),
                refundedAt = Utils.parseDateOpt(jsonTx.optStringNull("refundedAt")),
                isSandbox = jsonTx.getBoolean("isSandbox"),
            )
        }
    }
}
