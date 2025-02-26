package com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI

import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import org.json.JSONObject

class MapTransactionItem(val sku: String, val purchaseToken: String, val type: ProductType) {
    fun toJSON(): JSONObject {
        val result = JSONObject()
        result.put("sku", sku)
        result.put("purchaseToken", purchaseToken)
        result.put("type", type.name.lowercase())
        return result
    }
}