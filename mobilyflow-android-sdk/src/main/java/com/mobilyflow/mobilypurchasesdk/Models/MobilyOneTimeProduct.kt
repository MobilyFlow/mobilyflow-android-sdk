package com.mobilyflow.mobilypurchasesdk.Models

import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseRegistry
import com.mobilyflow.mobilypurchasesdk.Utils.StorePrice
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import org.json.JSONObject

class MobilyOneTimeProduct(
    val priceMillis: Int,
    val currencyCode: String,
    val priceFormatted: String,
    val isConsumable: Boolean,
    val isMultiQuantity: Boolean,
    val status: ProductStatus,
    // TODO: add android_purchaseOptionId (from android_basePlanId field)
) {
    companion object {
        internal fun parse(jsonProduct: JSONObject, currentRegion: String?): MobilyOneTimeProduct {
            val priceMillis: Int
            val currencyCode: String
            val priceFormatted: String
            val status: ProductStatus

            val androidProduct =
                MobilyPurchaseRegistry.getAndroidProduct(jsonProduct.getString("android_sku"))

            if (androidProduct?.oneTimePurchaseOfferDetails == null) {
                status =
                    if (androidProduct == null) ProductStatus.UNAVAILABLE else ProductStatus.INVALID

                val storePrice = StorePrice.getDefaultPrice(jsonProduct.getJSONArray("StorePrices"), currentRegion)
                priceMillis = storePrice?.priceMillis ?: 0
                currencyCode = storePrice?.currency ?: ""

                priceFormatted = Utils.formatPrice(priceMillis, currencyCode)
            } else {
                status = ProductStatus.AVAILABLE

                priceMillis = Utils.microToMillis(androidProduct.oneTimePurchaseOfferDetails!!.priceAmountMicros)
                currencyCode = androidProduct.oneTimePurchaseOfferDetails!!.priceCurrencyCode
                priceFormatted = androidProduct.oneTimePurchaseOfferDetails!!.formattedPrice
            }

            return MobilyOneTimeProduct(
                priceMillis = priceMillis,
                currencyCode = currencyCode,
                priceFormatted = priceFormatted,
                isConsumable = jsonProduct.getBoolean("isConsumable"),
                isMultiQuantity = jsonProduct.optBoolean("isMultiQuantity"),
                status = status,
            )
        }
    }
}