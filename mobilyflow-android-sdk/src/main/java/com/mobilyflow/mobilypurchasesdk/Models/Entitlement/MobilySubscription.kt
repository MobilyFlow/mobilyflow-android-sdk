package com.mobilyflow.mobilypurchasesdk.Models.Entitlement

import com.android.billingclient.api.Purchase
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.Platform
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilySubscriptionOffer
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import com.mobilyflow.mobilypurchasesdk.Utils.Utils.Companion.sha256
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject

class MobilySubscription(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val productId: String,
    val productOfferId: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val customerId: String,
    val platform: Platform,
    val environment: MobilyEnvironment,
    val renewProductId: String,
    val renewProductOfferId: String,
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

    val Product: MobilyProduct,
    val ProductOffer: MobilySubscriptionOffer?,
    val RenewProduct: MobilyProduct?,
    val RenewProductOffer: MobilySubscriptionOffer?,
) {
    companion object {
        internal fun parse(
            jsonSubscription: JSONObject,
            storeAccountTransactions: List<BillingClientWrapper.PurchaseWithType>?
        ): MobilySubscription {
            val platform = Platform.parse(jsonSubscription.getString("platform"))
            var autoRenewEnable = jsonSubscription.getBoolean("autoRenewEnable")
            var storeAccountTx: Purchase? = null

            var lastPlatformTxOriginalId: String? = jsonSubscription.optString("lastPlatformTxOriginalId")

            if (platform == Platform.ANDROID && lastPlatformTxOriginalId!!.isNotEmpty()) {
                val relatedPurchase = storeAccountTransactions?.find { tx ->
                    tx.type == ProductType.SUBSCRIPTION && sha256(tx.purchase.purchaseToken) == lastPlatformTxOriginalId
                }
                storeAccountTx = relatedPurchase?.purchase

                if (storeAccountTx != null) {
                    autoRenewEnable = storeAccountTx.isAutoRenewing
                    lastPlatformTxOriginalId = storeAccountTx.purchaseToken
                } else {
                    lastPlatformTxOriginalId = null
                }
            }

            val jsonProduct = jsonSubscription.getJSONObject("Product")
            val jsonProductOffer = jsonSubscription.optJSONObject("ProductOffer")
            val jsonRenewProduct = jsonSubscription.optJSONObject("RenewProduct")
            val jsonRenewProductOffer = jsonSubscription.optJSONObject("RenewProductOffer")

            val product = MobilyProduct.parse(jsonProduct)
            var productOffer: MobilySubscriptionOffer? = null
            var renewProduct: MobilyProduct? = null
            var renewProductOffer: MobilySubscriptionOffer? = null

            if (jsonProductOffer != null) {
                productOffer = MobilySubscriptionOffer.parse(
                    product.android_sku,
                    product.android_basePlanId,
                    jsonProduct,
                    jsonProductOffer
                )
            }

            if (jsonRenewProduct != null) {
                renewProduct = MobilyProduct.parse(jsonRenewProduct)

                // TODO: What if jsonRenewProduct is NULL but renewOffer is defined (change renew to same product but with an offer)
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
                productOfferId = jsonSubscription.getString("productOfferId"),
                startDate = Utils.parseDate(jsonSubscription.getString("startDate")),
                endDate = Utils.parseDate(jsonSubscription.getString("endDate")),
                customerId = jsonSubscription.getString("customerId"),
                platform = Platform.parse(jsonSubscription.getString("platform")),
                environment = MobilyEnvironment.parse(jsonSubscription.getString("environment")),
                renewProductId = jsonSubscription.getString("renewProductId"),
                renewProductOfferId = jsonSubscription.getString("renewProductOfferId"),
                lastPriceMillis = jsonSubscription.getInt("lastPriceMillis"),
                regularPriceMillis = jsonSubscription.getInt("regularPriceMillis"),
                renewPriceMillis = jsonSubscription.getInt("renewPriceMillis"),
                currency = jsonSubscription.getString("currency"),
                offerExpiryDate = Utils.parseDate(jsonSubscription.getString("offerExpiryDate")),
                offerRemainingCycle = jsonSubscription.getInt("offerRemainingCycle"),
                autoRenewEnable = autoRenewEnable,
                isInGracePeriod = jsonSubscription.getBoolean("isInGracePeriod"),
                isInBillingIssue = jsonSubscription.getBoolean("isInBillingIssue"),
                hasPauseScheduled = jsonSubscription.getBoolean("hasPauseScheduled"),
                isPaused = jsonSubscription.getBoolean("isPaused"),
                resumeDate = Utils.parseDate(jsonSubscription.getString("resumeDate")),
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