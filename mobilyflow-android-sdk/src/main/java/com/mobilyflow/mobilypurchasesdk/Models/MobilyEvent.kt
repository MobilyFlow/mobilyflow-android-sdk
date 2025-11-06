package com.mobilyflow.mobilypurchasesdk.Models

import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEventType
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyPlatform
import com.mobilyflow.mobilypurchasesdk.Models.Entitlement.MobilyItem
import com.mobilyflow.mobilypurchasesdk.Models.Entitlement.MobilySubscription
import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilySubscriptionOffer
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject

class MobilyEvent(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val transactionId: String,
    val subscriptionId: String,
    val itemId: String,
    val customerId: String,
    val type: MobilyEventType,
    val extras: JSONObject,
    val platform: MobilyPlatform,
    val environment: MobilyEnvironment,
    val isSandbox: Boolean,

    val Customer: MobilyCustomer?,
    val Product: MobilyProduct?,
    val ProductOffer: MobilySubscriptionOffer?,
    val Transaction: MobilyTransaction?,
    val Subscription: MobilySubscription?,
    val Item: MobilyItem?,
) {
    companion object {
        internal fun parse(
            jsonEvent: JSONObject,
            storeAccountTransactions: List<BillingClientWrapper.PurchaseWithType>?
        ): MobilyEvent {
            val jsonCustomer = jsonEvent.optJSONObject("Customer")
            val jsonProduct = jsonEvent.optJSONObject("Product")
            val jsonProductOffer = jsonEvent.optJSONObject("ProductOffer")
            val jsonTransaction = jsonEvent.optJSONObject("Transaction")
            val jsonSubscription = jsonEvent.optJSONObject("Subscription")
            val jsonItem = jsonEvent.optJSONObject("Item")

            var product: MobilyProduct? = null
            var productOffer: MobilySubscriptionOffer? = null

            if (jsonProduct != null) {
                product = MobilyProduct.parse(jsonProduct)

                if (jsonProductOffer != null) {
                    productOffer = MobilySubscriptionOffer.parse(
                        product.android_sku,
                        product.android_basePlanId,
                        jsonProduct,
                        jsonProductOffer
                    )
                }
            }

            return MobilyEvent(
                id = jsonEvent.getString("id"),
                createdAt = Utils.parseDate(jsonEvent.getString("createdAt")),
                updatedAt = Utils.parseDate(jsonEvent.getString("updatedAt")),
                transactionId = jsonEvent.getString("transactionId"),
                subscriptionId = jsonEvent.getString("subscriptionId"),
                itemId = jsonEvent.getString("itemId"),
                customerId = jsonEvent.getString("customerId"),
                type = MobilyEventType.parse(jsonEvent.getString("type")),
                extras = jsonEvent.getJSONObject("extras"),
                platform = MobilyPlatform.parse(jsonEvent.getString("platform")),
                environment = MobilyEnvironment.parse(jsonEvent.getString("environment")),
                isSandbox = jsonEvent.getBoolean("isSandbox"),

                Customer = if (jsonCustomer != null) MobilyCustomer.parse(jsonCustomer) else null,
                Product = product,
                ProductOffer = productOffer,
                Transaction = if (jsonTransaction != null) MobilyTransaction.parse(jsonTransaction) else null,
                Subscription = if (jsonSubscription != null) MobilySubscription.parse(
                    jsonSubscription,
                    storeAccountTransactions
                ) else null,
                Item = if (jsonItem != null) MobilyItem.parse(jsonItem) else null,
            )
        }
    }
}