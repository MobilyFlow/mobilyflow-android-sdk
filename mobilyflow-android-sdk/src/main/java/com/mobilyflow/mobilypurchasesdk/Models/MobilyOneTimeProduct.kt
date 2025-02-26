package com.mobilyflow.mobilypurchasesdk.Models

import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseRegistry
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import org.json.JSONObject

class MobilyOneTimeProduct(
    val price: Double,
    val currencyCode: String,
    val priceFormatted: String,
    val isConsumable: Boolean,
    val isMultiQuantity: Boolean,
    val status: ProductStatus,
) {
    companion object {
        internal fun parse(jsonProduct: JSONObject): MobilyOneTimeProduct {
            val price: Double
            val currencyCode: String
            val priceFormatted: String
            val status: ProductStatus

            val androidProduct =
                MobilyPurchaseRegistry.getAndroidProduct(jsonProduct.getString("android_sku"))

            if (androidProduct?.oneTimePurchaseOfferDetails == null) {
                status =
                    if (androidProduct == null) ProductStatus.UNAVAILABLE else ProductStatus.INVALID
                price = jsonProduct.optDouble("defaultPrice", 0.0)
                currencyCode = jsonProduct.optString("defaultCurrencyCode", "")
                priceFormatted = Utils.formatPrice(price, currencyCode)
            } else {
                status = ProductStatus.AVAILABLE

                price =
                    Utils.microToDouble(androidProduct.oneTimePurchaseOfferDetails!!.priceAmountMicros)
                currencyCode = androidProduct.oneTimePurchaseOfferDetails!!.priceCurrencyCode
                priceFormatted = androidProduct.oneTimePurchaseOfferDetails!!.formattedPrice
            }

            return MobilyOneTimeProduct(
                price = price,
                currencyCode = currencyCode,
                priceFormatted = priceFormatted,
                isConsumable = jsonProduct.getBoolean("isConsumable"),
                isMultiQuantity = jsonProduct.optBoolean("isMultiQuantity"),
                status = status,
            )
        }
    }
}