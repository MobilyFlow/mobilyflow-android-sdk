package com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI

import com.mobilyflow.mobilypurchasesdk.ApiHelper.ApiHelper
import com.mobilyflow.mobilypurchasesdk.ApiHelper.ApiRequest
import com.mobilyflow.mobilypurchasesdk.ApiHelper.ApiResponse
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.TransferOwnershipStatus
import com.mobilyflow.mobilypurchasesdk.Enums.WebhookStatus
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyTransferOwnershipException
import com.mobilyflow.mobilypurchasesdk.Utils.Utils.Companion.jsonArrayToStringArray
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class MobilyPurchaseAPI(
    val appId: String,
    private val apiKey: String,
    val environment: MobilyEnvironment,
    languages: Array<String>,
    apiURL: String? = null
) {
    val API_URL = apiURL ?: "https://api.mobilyflow.com/v1/"
    val helper: ApiHelper = ApiHelper(API_URL, mapOf("Authorization" to "Bearer $apiKey"))
    val lang = languages.joinToString(",")

    /**
     * Log user into MobilyFlow with his externalId and return his uuid.
     * Throws on error.
     */
    @Throws(MobilyException::class)
    fun login(externalId: String): LoginResponse {
        val response: ApiResponse?
        try {
            val data = JSONObject()
                .put("externalId", externalId)
                .put("environment", environment.value)

            response = this.helper.request(
                ApiRequest("POST", "/apps/me/customers/login/android").setData(data)
            )
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        return LoginResponse(
            response.json().getString("id"),
            jsonArrayToStringArray(response.json().getJSONArray("platformOriginalTransactionIds")),
        )
    }

    /**
     * Get products in JSONArray format
     */
    @Throws(MobilyException::class)
    fun getProducts(identifiers: Array<String>?): JSONArray {
        if (identifiers != null && identifiers.isEmpty()) {
            return JSONArray()
        }

        val request = ApiRequest("GET", "/apps/me/products")
        request.addParam("environment", environment.value)
        request.addParam("lang", this.lang)

        if (identifiers != null) {
            request.addParam("identifiers", identifiers.joinToString(","))
        }

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        return response.jsonArray()
    }

    /**
     * Get entitlements
     */
    @Throws(MobilyException::class)
    fun getCustomerEntitlements(customerId: String): JSONArray {
        val request = ApiRequest("GET", "/apps/me/customers/${customerId}/entitlements")
        request.addParam("lang", this.lang)

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        return response.jsonArray()
    }


    @Throws(MobilyException::class)
    fun mapTransactions(customerId: String, transactions: Array<MapTransactionItem>) {
        val response: ApiResponse?
        try {
            val jsonTransactions = JSONArray()
            for (tx in transactions) {
                jsonTransactions.put(tx.toJSON())
            }

            val data = JSONObject()
                .put("customerId", customerId)
                .put("transactions", jsonTransactions)

            response = this.helper.request(
                ApiRequest("POST", "/apps/me/customers/mappings/android").setData(data)
            )
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }
    }

    @Throws(MobilyException::class)
    fun transferOwnershipRequest(customerId: String, transactions: Array<String>): String {
        val response: ApiResponse?
        try {
            val jsonTransactions = JSONArray()
            for (tx in transactions) {
                jsonTransactions.put(tx)
            }

            val data = JSONObject()
                .put("customerId", customerId)
                .put("transactions", jsonTransactions)

            response = this.helper.request(
                ApiRequest(
                    "POST",
                    "/apps/me/customers/transfer-ownership/request/android"
                ).setData(data)
            )
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        val jsonResponse = response.json()
        if (response.success) {
            return jsonResponse.getString("id")
        } else {
            var errorType: MobilyTransferOwnershipException.Type? = try {
                MobilyTransferOwnershipException.Type.valueOf(
                    jsonResponse.getString("errorCode").uppercase()
                )
            } catch (_: Exception) {
                null
            }

            if (errorType != null) {
                throw MobilyTransferOwnershipException(errorType)
            } else {
                throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
            }
        }
    }

    /**
     * Get transfer ownership request status from requestId
     */
    @Throws(MobilyException::class)
    fun getTransferRequestStatus(requestId: String): TransferOwnershipStatus {
        val request = ApiRequest("GET", "/apps/me/customers/transfer-ownership/${requestId}/status")

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (response.success) {
            val jsonResponse = response.json()
            return TransferOwnershipStatus.valueOf(jsonResponse.getString("status").uppercase())
        } else {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }
    }

    /**
     * Get webhook status from transactionID
     */
    @Throws(MobilyException::class)
    fun getWebhookStatus(transactionId: String): WebhookStatus {
        val request = ApiRequest("GET", "/apps/me/events/webhook-status/android")
        request.addParam("platformTxId", transactionId)

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        val jsonResponse = response.json()
        return WebhookStatus.valueOf(jsonResponse.getString("status").uppercase())
    }

    /**
     * Upload monitoring file
     */
    @Throws(MobilyException::class)
    fun uploadMonitoring(customerId: String?, file: File) {
        val request = ApiRequest("POST", "/apps/me/monitoring/upload")
        if (customerId != null) {
            request.addData("customerId", customerId)
        }
        request.addFile("logFile", file)

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }
    }
}