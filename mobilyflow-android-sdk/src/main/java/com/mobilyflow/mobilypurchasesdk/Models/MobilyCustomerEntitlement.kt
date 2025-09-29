package com.mobilyflow.mobilypurchasesdk.Models

import com.android.billingclient.api.Purchase
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.Platform
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import com.mobilyflow.mobilypurchasesdk.Utils.Utils.Companion.sha256
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject

class MobilyCustomerEntitlement(
    val type: ProductType,
    val product: MobilyProduct,
    /**
     * If the entitlement was made on Android, it's a sha256 of the purchase token for security reason,
     * on iOS it's the originalTransactionId
     */
    val platformOriginalTransactionId: String?,
    val item: ItemEntitlement?,
    val subscription: SubscriptionEntitlement?,
    val customerId: String,
) {
    companion object {
        internal fun parse(
            jsonEntitlement: JSONObject,
            storeAccountTransactions: List<BillingClientWrapper.PurchaseWithType>?,
            currentRegion: String?
        ): MobilyCustomerEntitlement {
            val type = ProductType.valueOf(jsonEntitlement.getString("type").uppercase())
            val jsonEntity = jsonEntitlement.getJSONObject("entity")
            val product = MobilyProduct.parse(jsonEntity.getJSONObject("Product"), currentRegion)
            val platformOriginalTransactionId = jsonEntitlement.optString("platformOriginalTransactionId")

            var item: ItemEntitlement? = null
            var subscription: SubscriptionEntitlement? = null
            val customerId = jsonEntity.getString("customerId")

            if (type == ProductType.ONE_TIME) {
                item = ItemEntitlement(jsonEntity.getInt("quantity"))
            } else {
                val platform = Platform.valueOf(jsonEntity.getString("platform").uppercase())
                var storeAccountTx: Purchase? = null
                var autoRenewEnable = jsonEntity.getBoolean("autoRenewEnable")

                if (platform == Platform.ANDROID && platformOriginalTransactionId.isNotEmpty()) {
                    val relatedPurchase = storeAccountTransactions?.find { tx ->
                        tx.type == ProductType.SUBSCRIPTION && sha256(tx.purchase.purchaseToken) == platformOriginalTransactionId
                    }
                    storeAccountTx = relatedPurchase?.purchase
                }

                if (storeAccountTx != null) {
                    autoRenewEnable = storeAccountTx.isAutoRenewing
                }

                val productOfferJson = jsonEntity.optJSONObject("ProductOffer")
                val renewProductJson = jsonEntity.optJSONObject("RenewProduct")
                val renewProductOfferJson = jsonEntity.optJSONObject("RenewProductOffer")

                subscription = SubscriptionEntitlement(
                    startDate = Utils.parseDate(jsonEntity.getString("startDate")),
                    endDate = Utils.parseDate(jsonEntity.getString("endDate")),
                    autoRenewEnable = autoRenewEnable,
                    isInGracePeriod = jsonEntity.getBoolean("isInGracePeriod"),
                    isInBillingIssue = jsonEntity.getBoolean("isInBillingIssue"),
                    isExpiredOrRevoked = jsonEntity.getBoolean("isExpiredOrRevoked"),
                    isPaused = jsonEntity.getBoolean("isPaused"),
                    hasPauseScheduled = jsonEntity.getBoolean("hasPauseScheduled"),
                    resumeDate = if (jsonEntity.isNull("endDate")) null else Utils.parseDate(jsonEntity.getString("endDate")),
                    offerExpiryDate = if (jsonEntity.isNull("offerExpiryDate")) null else Utils.parseDate(
                        jsonEntity.getString(
                            "offerExpiryDate"
                        )
                    ),
                    offerRemainingCycle = jsonEntity.getInt("offerRemainingCycle"),
                    currency = jsonEntity.getString("currency"),
                    lastPriceMillis = jsonEntity.getInt("lastPriceMillis"),
                    regularPriceMillis = jsonEntity.getInt("regularPriceMillis"),
                    renewPriceMillis = jsonEntity.getInt("renewPriceMillis"),
                    platform = Platform.valueOf(jsonEntity.getString("platform").uppercase()),
                    isManagedByThisStoreAccount = storeAccountTx != null,
                    offer = if (productOfferJson != null) MobilySubscriptionOffer.parse(
                        sku = product.android_sku,
                        basePlanId = product.subscriptionProduct!!.android_basePlanId,
                        jsonBase = jsonEntity.getJSONObject("Product"),
                        jsonOffer = productOfferJson,
                        currentRegion = currentRegion
                    ) else null,
                    renewProduct = if (renewProductJson != null) MobilyProduct.parse(
                        renewProductJson,
                        currentRegion
                    ) else null,
                    renewProductOffer = if (renewProductJson != null && renewProductOfferJson != null) MobilySubscriptionOffer.parse(
                        sku = renewProductJson.getString("android_sku"),
                        basePlanId = renewProductJson.getString("android_basePlanId"),
                        jsonBase = renewProductJson,
                        jsonOffer = renewProductOfferJson,
                        currentRegion = currentRegion
                    ) else null,
                    purchaseToken = storeAccountTx?.purchaseToken,
                )
            }

            return MobilyCustomerEntitlement(
                type = type,
                product = product,
                platformOriginalTransactionId = platformOriginalTransactionId,
                item = item,
                subscription = subscription,
                customerId = customerId,
            )
        }
    }

    class ItemEntitlement(val quantity: Int) {}

    class SubscriptionEntitlement(
        val startDate: LocalDateTime,
        val endDate: LocalDateTime,
        val autoRenewEnable: Boolean,
        val isInGracePeriod: Boolean,
        val isInBillingIssue: Boolean,
        val isExpiredOrRevoked: Boolean,
        val isPaused: Boolean,
        val hasPauseScheduled: Boolean,
        val resumeDate: LocalDateTime?,
        val offerExpiryDate: LocalDateTime?,
        val offerRemainingCycle: Int,
        val currency: String,
        val lastPriceMillis: Int,
        val regularPriceMillis: Int,
        val renewPriceMillis: Int,
        val platform: Platform,
        val isManagedByThisStoreAccount: Boolean,
        val offer: MobilySubscriptionOffer?,
        val renewProduct: MobilyProduct?,
        val renewProductOffer: MobilySubscriptionOffer?,
        val purchaseToken: String?,
    ) {}
}