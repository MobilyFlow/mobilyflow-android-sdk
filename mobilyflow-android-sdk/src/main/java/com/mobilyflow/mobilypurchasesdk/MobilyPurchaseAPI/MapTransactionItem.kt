package com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI

import com.mobilyflow.mobilypurchasesdk.Enums.MobilyProductType
import org.json.JSONObject

class MapTransactionItem(val sku: String, val purchaseToken: String, val type: MobilyProductType) {
    fun toJSON(): JSONObject {
        val result = JSONObject()
        result.put("sku", sku)
        result.put("purchaseToken", purchaseToken)
        result.put("type", type.name.lowercase())
        return result
    }
}