package com.mobilyflow.mobilypurchasesdk.Models

import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import org.json.JSONObject

class MobilySubscriptionGroup(
    val id: String,
    val identifier: String,
    val name: String,
    val description: String,
    val extras: JSONObject?,
    var products: List<MobilyProduct>
) {
    companion object {
        internal fun parse(
            jsonGroup: JSONObject,
            onlyAvailableProducts: Boolean = false
        ): MobilySubscriptionGroup {
            val group = MobilySubscriptionGroup(
                id = jsonGroup.getString("id"),
                identifier = jsonGroup.getString("identifier"),
                name = jsonGroup.optString("name"),
                description = jsonGroup.optString("description") ?: "",
                extras = jsonGroup.optJSONObject("extras"),
                products = arrayListOf(),
            )

            if (jsonGroup.has("Products")) {
                val jsonProducts = jsonGroup.getJSONArray("Products")
                for (i in 0..<jsonProducts.length()) {
                    val product = MobilyProduct.parse(jsonProducts.getJSONObject(i))

                    if (!onlyAvailableProducts || product.status === ProductStatus.AVAILABLE) {
                        (group.products as ArrayList).add(product)
                    }
                }
            }

            return group
        }
    }
}