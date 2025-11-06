package com.mobilyflow.mobilypurchasesdk.Models.Entitlement

import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject

class MobilyItem(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime,
    val environment: MobilyEnvironment,
    val productId: String,
    val customerId: String,
    val quantity: Int,
    val Product: MobilyProduct,
) {
    companion object {
        internal fun parse(jsonItem: JSONObject): MobilyItem {
            return MobilyItem(
                id = jsonItem.getString("id"),
                createdAt = Utils.parseDate(jsonItem.getString("createdAt")),
                updatedAt = Utils.parseDate(jsonItem.getString("updatedAt")),
                deletedAt = Utils.parseDate(jsonItem.getString("deletedAt")),
                environment = MobilyEnvironment.parse(jsonItem.getString("environment")),
                productId = jsonItem.getString("productId"),
                customerId = jsonItem.getString("customerId"),
                quantity = jsonItem.getInt("quantity"),
                Product = MobilyProduct.parse(jsonItem.getJSONObject("Product")),
            )
        }
    }
}