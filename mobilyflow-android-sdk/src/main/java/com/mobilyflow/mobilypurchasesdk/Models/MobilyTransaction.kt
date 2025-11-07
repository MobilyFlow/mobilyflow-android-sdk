package com.mobilyflow.mobilypurchasesdk.Models

import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyTransactionStatus
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyPlatform
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import com.mobilyflow.mobilypurchasesdk.Utils.optStringNull
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject

class MobilyTransaction(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val platformTxId: String,
    val platformTxOriginalId: String,
    val customerId: String,
    val quantity: Int,
    val country: String,
    val priceMillis: Int,
    val currency: String,
    val convertedPriceMillis: Int,
    val convertedCurrency: String,
    val status: MobilyTransactionStatus,
    val refundedPercent: Double,
    val productId: String,
    val subscriptionId: String,
    val itemId: String,
    val productOfferId: String,
    val platform: MobilyPlatform,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val refundDate: LocalDateTime?,
    val isSandbox: Boolean,
) {
    companion object {
        internal fun parse(jsonTx: JSONObject): MobilyTransaction {
            return MobilyTransaction(
                id = jsonTx.getString("id"),
                createdAt = Utils.parseDate(jsonTx.getString("createdAt")),
                updatedAt = Utils.parseDate(jsonTx.getString("updatedAt")),
                platformTxId = jsonTx.getString("platformTxId"),
                platformTxOriginalId = jsonTx.getString("platformTxOriginalId"),
                customerId = jsonTx.getString("customerId"),
                quantity = jsonTx.optInt("quantity", 1),
                country = jsonTx.getString("country"),
                priceMillis = jsonTx.getInt("priceMillis"),
                currency = jsonTx.getString("currency"),
                convertedPriceMillis = jsonTx.getInt("convertedPriceMillis"),
                convertedCurrency = jsonTx.getString("convertedCurrency"),
                status = MobilyTransactionStatus.parse(jsonTx.getString("status")),
                refundedPercent = jsonTx.getDouble("refundedPercent"),
                productId = jsonTx.getString("productId"),
                subscriptionId = jsonTx.getString("subscriptionId"),
                itemId = jsonTx.getString("itemId"),
                productOfferId = jsonTx.getString("productOfferId"),
                platform = MobilyPlatform.parse(jsonTx.getString("platform")),
                startDate = Utils.parseDate(jsonTx.getString("startDate")),
                endDate = Utils.parseDate(jsonTx.getString("endDate")),
                refundDate = Utils.parseDateOpt(jsonTx.optStringNull("refundDate")),
                isSandbox = jsonTx.getBoolean("isSandbox"),
            )
        }
    }
}
