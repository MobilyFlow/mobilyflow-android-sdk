package com.mobilyflow.mobilypurchasesdk.Utils

import com.mobilyflow.mobilypurchasesdk.Enums.Platform
import org.json.JSONArray
import org.json.JSONObject


class StorePrice(
    val priceMillis: Int,
    val currency: String,
    val regionCode: String,
    val platform: Platform?
) {
    companion object {
        fun parse(storePrice: JSONObject): StorePrice {
            val platform = storePrice.optString("platform")

            return StorePrice(
                priceMillis = storePrice.getInt("priceMillis"),
                currency = storePrice.getString("currency"),
                regionCode = storePrice.getString("regionCode"),
                platform = if (platform.isEmpty()) null else Platform.valueOf(platform.uppercase())
            )
        }

        fun getDefaultPrice(storePrices: JSONArray?, currentRegion: String?): StorePrice? {
            if (storePrices != null) {
                // 1. Try getting currentRegion price

                for (i in 0..<storePrices.length()) {
                    val price = storePrices.getJSONObject(i)
                    val platform = price.optString("platform")
                    if (price.getString("regionCode") == currentRegion && (platform.isEmpty() || platform == "android")) {
                        return parse(price)
                    }
                }

                // 2. Try getting price flagged "isDefault"
                for (i in 0..<storePrices.length()) {
                    val price = storePrices.getJSONObject(i)
                    val platform = price.optString("platform")
                    if (price.optBoolean("isDefault") && (platform.isEmpty() || platform == "android")) {
                        return parse(price)
                    }
                }

                // 3. Try getting first price that match Android platform
                for (i in 0..<storePrices.length()) {
                    val price = storePrices.getJSONObject(i)
                    val platform = price.optString("platform")
                    if (platform.isEmpty() || platform == "android") {
                        return parse(price)
                    }
                }
            }

            // Else return null
            return null
        }
    }
}