package com.mobilyflow.mobilypurchasesdk.Models.Product

import com.mobilyflow.mobilypurchasesdk.Enums.MobilyProductStatus
import com.mobilyflow.mobilypurchasesdk.Utils.TranslationUtils
import org.json.JSONObject

class MobilySubscriptionGroup(
    val id: String,
    val identifier: String,
    val referenceName: String,
    val name: String,
    val description: String,
    val extras: JSONObject?,
    var Products: List<MobilyProduct>
) {
    companion object {
        internal fun parse(
            jsonGroup: JSONObject,
            onlyAvailableProducts: Boolean = false
        ): MobilySubscriptionGroup {
            val group = MobilySubscriptionGroup(
                id = jsonGroup.getString("id"),
                identifier = jsonGroup.getString("identifier"),
                referenceName = jsonGroup.getString("referenceName"),
                name = TranslationUtils.getTranslationValue(jsonGroup.optJSONArray("_translations"), "name") ?: "",
                description = TranslationUtils.getTranslationValue(
                    jsonGroup.optJSONArray("_translations"),
                    "description"
                ) ?: "",
                extras = jsonGroup.optJSONObject("extras"),
                Products = arrayListOf(),
            )

            if (jsonGroup.has("Products")) {
                val jsonProducts = jsonGroup.getJSONArray("Products")

                for (i in 0..<jsonProducts.length()) {
                    val product = MobilyProduct.parse(jsonProducts.getJSONObject(i))

                    if (!onlyAvailableProducts || product.status === MobilyProductStatus.AVAILABLE) {
                        (group.Products as ArrayList).add(product)
                    }
                }
            }

            return group
        }
    }
}