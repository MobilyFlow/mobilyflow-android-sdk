package com.mobilyflow.mobilypurchasesdk.Utils

import com.mobilyflow.mobilypurchasesdk.Enums.MobilyPlatform
import org.json.JSONObject


class StorePrice(
    val priceMillis: Int,
    val currency: String,
    val regionCode: String,
    val platform: MobilyPlatform
) {
    companion object {
        fun parse(storePrice: JSONObject): StorePrice {
            val platform = storePrice.optString("platform")

            return StorePrice(
                priceMillis = storePrice.getInt("priceMillis"),
                currency = storePrice.getString("currency"),
                regionCode = storePrice.getString("regionCode"),
                platform = MobilyPlatform.parse(platform)
            )
        }
    }
}