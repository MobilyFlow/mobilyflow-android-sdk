package com.mobilyflow.mobilypurchasesdk.Models

import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MobilyProduct(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val identifier: String,
    val appId: String,

    val name: String,
    val description: String,

    val android_sku: String,
    val type: ProductType,
    val extras: JSONObject?,

    val status: ProductStatus,
    val oneTimeProduct: MobilyOneTimeProduct?,
    val subscriptionProduct: MobilySubscriptionProduct?,
) {
    companion object {
        internal fun parse(jsonProduct: JSONObject): MobilyProduct {
            val type = ProductType.valueOf(jsonProduct.getString("type").uppercase())
            val status: ProductStatus
            var oneTimeProduct: MobilyOneTimeProduct? = null
            var subscriptionProduct: MobilySubscriptionProduct? = null


            if (type == ProductType.ONE_TIME) {
                oneTimeProduct = MobilyOneTimeProduct.parse(jsonProduct)
                status = oneTimeProduct.status
            } else {
                subscriptionProduct = MobilySubscriptionProduct.parse(jsonProduct)
                status = subscriptionProduct.status
            }

            val product = MobilyProduct(
                id = jsonProduct.getString("id"),
                createdAt = LocalDateTime.parse(
                    jsonProduct.getString("createdAt"),
                    DateTimeFormatter.ISO_DATE_TIME
                ),
                updatedAt = LocalDateTime.parse(
                    jsonProduct.getString("updatedAt"),
                    DateTimeFormatter.ISO_DATE_TIME
                ),
                identifier = jsonProduct.getString("identifier"),
                appId = jsonProduct.getString("appId"),

                name = jsonProduct.getString("name"),
                description = jsonProduct.getString("description"),

                android_sku = jsonProduct.getString("android_sku"),
                type = type,
                extras = jsonProduct.optJSONObject("extras"),

                status = status,
                oneTimeProduct = oneTimeProduct,
                subscriptionProduct = subscriptionProduct,
            )

            return product
        }
    }
}