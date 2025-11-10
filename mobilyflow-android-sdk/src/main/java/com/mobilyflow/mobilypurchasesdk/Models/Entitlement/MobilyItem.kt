package com.mobilyflow.mobilypurchasesdk.Models.Entitlement

import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject

class MobilyItem(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val productId: String,
    val quantity: Int,
    val Product: MobilyProduct?,
) {
    companion object {
        internal fun parse(jsonItem: JSONObject): MobilyItem {
            val jsonProduct = jsonItem.optJSONObject("Product")

            return MobilyItem(
                id = jsonItem.getString("id"),
                createdAt = Utils.parseDate(jsonItem.getString("createdAt")),
                updatedAt = Utils.parseDate(jsonItem.getString("updatedAt")),
                productId = jsonItem.getString("productId"),
                quantity = jsonItem.getInt("quantity"),
                Product = if (jsonProduct != null) MobilyProduct.parse(jsonProduct) else null,
            )
        }
    }
}






