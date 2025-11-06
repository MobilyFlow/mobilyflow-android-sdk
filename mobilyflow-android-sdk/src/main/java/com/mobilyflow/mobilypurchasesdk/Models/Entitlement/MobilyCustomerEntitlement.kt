package com.mobilyflow.mobilypurchasesdk.Models.Entitlement

import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyProductType
import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilyProduct
import org.json.JSONObject

class MobilyCustomerEntitlement(
    val type: MobilyProductType,
    val Product: MobilyProduct,
    val Item: MobilyItem?,
    val Subscription: MobilySubscription?,
    val customerId: String,
) {
    companion object {
        internal fun parse(
            jsonEntitlement: JSONObject,
            storeAccountTransactions: List<BillingClientWrapper.PurchaseWithType>?,
        ): MobilyCustomerEntitlement {
            val type = MobilyProductType.parse(jsonEntitlement.getString("type"))
            val jsonEntity = jsonEntitlement.getJSONObject("entity")
            val product: MobilyProduct

            var item: MobilyItem? = null
            var subscription: MobilySubscription? = null

            if (type == MobilyProductType.ONE_TIME) {
                item = MobilyItem.parse(jsonEntity)
                product = item.Product
            } else {
                subscription = MobilySubscription.parse(jsonEntity, storeAccountTransactions)
                product = subscription.Product
            }

            return MobilyCustomerEntitlement(
                type = type,
                Product = product,
                Item = item,
                Subscription = subscription,
                customerId = jsonEntity.getString("customerId"),
            )
        }
    }
}