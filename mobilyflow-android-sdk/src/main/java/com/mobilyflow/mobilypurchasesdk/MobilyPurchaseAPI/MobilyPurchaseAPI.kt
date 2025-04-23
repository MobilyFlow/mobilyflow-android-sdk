package com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI

import com.mobilyflow.mobilypurchasesdk.ApiHelper.ApiHelper
import com.mobilyflow.mobilypurchasesdk.ApiHelper.ApiRequest
import com.mobilyflow.mobilypurchasesdk.ApiHelper.ApiResponse
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.TransferOwnershipStatus
import com.mobilyflow.mobilypurchasesdk.Enums.WebhookStatus
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyTransferOwnershipException
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
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
    val helper: ApiHelper = ApiHelper(API_URL, mapOf("Authorization" to "ApiKey $apiKey"))
    val lang = languages.joinToString(",")

    /**
     * Log user into MobilyFlow with his externalRef and return his uuid.
     * Throws on error.
     */
    @Throws(MobilyException::class)
    fun login(externalRef: String): LoginResponse {
        val response: ApiResponse?
        try {
            val data = JSONObject()
                .put("externalRef", externalRef)
                .put("environment", environment.toString().lowercase())

            response = this.helper.request(
                ApiRequest("POST", "/apps/me/customers/login/android").setData(data)
            )
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            Logger.w("[login] API Error: ${response.string()}")
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        val jsonResponse = response.json().getJSONObject("data")
        return LoginResponse(
            jsonResponse.getString("id"),
            jsonArrayToStringArray(jsonResponse.getJSONArray("platformOriginalTransactionIds")),
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
        request.addParam("environment", environment.toString().lowercase())
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
            Logger.w("[getProducts] API Error: ${response.string()}")
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        return response.json().getJSONArray("data")
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
            Logger.w("[getCustomerEntitlements] API Error: ${response.string()}")
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        return response.json().getJSONArray("data")
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
            Logger.w("[mapTransactions] API Error: ${response.string()}")
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
            return jsonResponse.getJSONObject("data").getString("id")
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
                Logger.w("[transferOwnershipRequest] API Error: ${response.string()}")
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
            val jsonResponse = response.json().getJSONObject("data")
            return TransferOwnershipStatus.valueOf(jsonResponse.getString("status").uppercase())
        } else {
            Logger.w("[getTransferRequestStatus] API Error: ${response.string()}")
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
            Logger.w("[getWebhookStatus] API Error: ${response.string()}")
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        val jsonResponse = response.json().getJSONObject("data")
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
            Logger.w("[uploadMonitoring] API Error: ${response.string()}")
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }
    }

    @Throws(MobilyException::class)
    fun isForwardingEnable(customerId: String?): Boolean {
        val request = ApiRequest("GET", "/apps/me/customers/is-forwarding-enable")
        if (customerId != null) {
            request.addParam("customerId", customerId)
        }
        request.addParam("platform", "android")

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            Logger.w("[isForwardingEnable] API Error: ${response.string()}")
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        val jsonResponse = response.json().getJSONObject("data")
        return jsonResponse.getBoolean("enable")
    }
}