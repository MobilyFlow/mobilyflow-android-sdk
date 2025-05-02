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
) {
    companion object {
        internal fun parse(
            jsonEntitlement: JSONObject,
            storeAccountTransactions: List<BillingClientWrapper.PurchaseWithType>?
        ): MobilyCustomerEntitlement {
            val type = ProductType.valueOf(jsonEntitlement.getString("type").uppercase())
            val jsonEntity = jsonEntitlement.getJSONObject("entity")
            val product = MobilyProduct.parse(jsonEntity.getJSONObject("Product"))
            val platformOriginalTransactionId = jsonEntitlement.optString("platformOriginalTransactionId")

            var item: ItemEntitlement? = null
            var subscription: SubscriptionEntitlement? = null

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

                val renewProductJson = jsonEntity.optJSONObject("RenewProduct")

                subscription = SubscriptionEntitlement(
                    startDate = Utils.parseDate(jsonEntity.getString("startDate")),
                    expirationDate = Utils.parseDate(jsonEntity.getString("expirationDate")),
                    autoRenewEnable = autoRenewEnable,
                    platform = Platform.valueOf(jsonEntity.getString("platform").uppercase()),
                    isManagedByThisStoreAccount = storeAccountTx != null,
                    renewProduct = if (renewProductJson != null) MobilyProduct.parse(renewProductJson) else null,
                    purchaseToken = storeAccountTx?.purchaseToken,
                )
            }

            return MobilyCustomerEntitlement(
                type = type,
                product = product,
                platformOriginalTransactionId = platformOriginalTransactionId,
                item = item,
                subscription = subscription,
            )
        }
    }

    class ItemEntitlement(val quantity: Int) {}

    class SubscriptionEntitlement(
        val startDate: LocalDateTime,
        val expirationDate: LocalDateTime,
        val autoRenewEnable: Boolean,
        val platform: Platform,
        val isManagedByThisStoreAccount: Boolean,
        val renewProduct: MobilyProduct?,
        val purchaseToken: String?,
    ) {}
}