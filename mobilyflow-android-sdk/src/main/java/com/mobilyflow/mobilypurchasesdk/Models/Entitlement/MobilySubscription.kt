package com.mobilyflow.mobilypurchasesdk.Models.Entitlement

import com.android.billingclient.api.Purchase
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyPlatform
import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilySubscriptionOffer
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import com.mobilyflow.mobilypurchasesdk.Utils.optStringNull
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject

class MobilySubscription(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val productId: String,
    val productOfferId: String?,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val platform: MobilyPlatform,
    val renewProductId: String?,
    val renewProductOfferId: String?,
    val lastPriceMillis: Int,
    val regularPriceMillis: Int,
    val renewPriceMillis: Int,
    val currency: String,
    val offerExpiryDate: LocalDateTime?,
    val offerRemainingCycle: Int,
    val autoRenewEnable: Boolean,
    val isInGracePeriod: Boolean,
    val isInBillingIssue: Boolean,
    val hasPauseScheduled: Boolean,
    val isPaused: Boolean,
    val resumeDate: LocalDateTime?,
    val isExpiredOrRevoked: Boolean,

    val isManagedByThisStoreAccount: Boolean,
    val lastPlatformTxOriginalId: String?,

    val Product: MobilyProduct?,
    val ProductOffer: MobilySubscriptionOffer?,
    val RenewProduct: MobilyProduct?,
    val RenewProductOffer: MobilySubscriptionOffer?,
) {
    companion object {
        internal fun parse(
            jsonSubscription: JSONObject,
            storeAccountTransactions: List<BillingClientWrapper.PurchaseWithType>?
        ): MobilySubscription {
            val platform = MobilyPlatform.parse(jsonSubscription.getString("platform"))
            var autoRenewEnable = jsonSubscription.getBoolean("autoRenewEnable")

            var lastPlatformTxOriginalId = jsonSubscription.optStringNull("lastPlatformTxOriginalId")
            var storeAccountTx: Purchase? = null

            if (platform == MobilyPlatform.ANDROID) {
                val relatedPurchase =
                    Utils.getPurchaseWithSha256PurchaseToken(lastPlatformTxOriginalId, storeAccountTransactions)
                storeAccountTx = relatedPurchase?.purchase
                lastPlatformTxOriginalId = storeAccountTx?.purchaseToken

                if (storeAccountTx != null) {
                    autoRenewEnable = storeAccountTx.isAutoRenewing
                }
            }

            val jsonProduct = jsonSubscription.optJSONObject("Product")
            val product = if (jsonProduct != null) MobilyProduct.parse(jsonProduct) else null

            val jsonProductOffer = jsonSubscription.optJSONObject("ProductOffer")
            val jsonRenewProduct = jsonSubscription.optJSONObject("RenewProduct")
            val jsonRenewProductOffer = jsonSubscription.optJSONObject("RenewProductOffer")

            var productOffer: MobilySubscriptionOffer? = null
            var renewProduct: MobilyProduct? = null
            var renewProductOffer: MobilySubscriptionOffer? = null

            if (product != null && jsonProductOffer != null) {
                productOffer = MobilySubscriptionOffer.parse(
                    product.android_sku,
                    product.android_basePlanId,
                    jsonProduct,
                    jsonProductOffer
                )
            }

            if (jsonRenewProduct != null) {
                renewProduct = MobilyProduct.parse(jsonRenewProduct)

                if (jsonRenewProductOffer != null) {
                    renewProductOffer = MobilySubscriptionOffer.parse(
                        renewProduct.android_sku,
                        renewProduct.android_basePlanId,
                        jsonRenewProduct,
                        jsonRenewProductOffer
                    )
                }
            }

            return MobilySubscription(
                id = jsonSubscription.getString("id"),
                createdAt = Utils.parseDate(jsonSubscription.getString("createdAt")),
                updatedAt = Utils.parseDate(jsonSubscription.getString("updatedAt")),
                productId = jsonSubscription.getString("productId"),
                productOfferId = jsonSubscription.optStringNull("productOfferId"),
                startDate = Utils.parseDate(jsonSubscription.getString("startDate")),
                endDate = Utils.parseDate(jsonSubscription.getString("endDate")),
                platform = platform,
                renewProductId = jsonSubscription.optStringNull("renewProductId"),
                renewProductOfferId = jsonSubscription.optStringNull("renewProductOfferId"),
                lastPriceMillis = jsonSubscription.getInt("lastPriceMillis"),
                regularPriceMillis = jsonSubscription.getInt("regularPriceMillis"),
                renewPriceMillis = jsonSubscription.getInt("renewPriceMillis"),
                currency = jsonSubscription.getString("currency"),
                offerExpiryDate = Utils.parseDateOpt(jsonSubscription.optStringNull("offerExpiryDate")),
                offerRemainingCycle = jsonSubscription.optInt("offerRemainingCycle"),
                autoRenewEnable = autoRenewEnable,
                isInGracePeriod = jsonSubscription.getBoolean("isInGracePeriod"),
                isInBillingIssue = jsonSubscription.getBoolean("isInBillingIssue"),
                hasPauseScheduled = jsonSubscription.getBoolean("hasPauseScheduled"),
                isPaused = jsonSubscription.getBoolean("isPaused"),
                resumeDate = Utils.parseDateOpt(jsonSubscription.optStringNull("resumeDate")),
                isExpiredOrRevoked = jsonSubscription.getBoolean("isExpiredOrRevoked"),

                isManagedByThisStoreAccount = storeAccountTx != null,
                lastPlatformTxOriginalId = lastPlatformTxOriginalId,

                Product = product,
                ProductOffer = productOffer,
                RenewProduct = renewProduct,
                RenewProductOffer = renewProductOffer,
            )
        }
    }
}
