package com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI

import android.content.Context
import com.mobilyflow.mobilypurchasesdk.ApiHelper.ApiHelper
import com.mobilyflow.mobilypurchasesdk.ApiHelper.ApiRequest
import com.mobilyflow.mobilypurchasesdk.ApiHelper.ApiResponse
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyProductType
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyTransferOwnershipStatus
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyWebhookStatus
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyTransferOwnershipException
import com.mobilyflow.mobilypurchasesdk.MOBILYFLOW_SDK_VERSION
import com.mobilyflow.mobilypurchasesdk.Models.Internal.MobilyWebhookResult
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.Utils.DeviceInfo
import com.mobilyflow.mobilypurchasesdk.Utils.Utils.Companion.jsonArrayToStringArray
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class MobilyPurchaseAPI(
    val appId: String,
    private val apiKey: String,
    val environment: MobilyEnvironment,
    locales: Array<String>,
    apiURL: String?,
    val getCurrentRegion: () -> String?
) {
    val API_URL = apiURL ?: "https://api.mobilyflow.com/v1/"
    val locale = locales.joinToString(",")

    val helper: ApiHelper = ApiHelper(
        API_URL, mapOf(
            "Authorization" to "ApiKey $apiKey",
            "platform" to "android",
            "sdk_version" to MOBILYFLOW_SDK_VERSION
        )
    )

    /**
     * Get AppPlatform (with ForceUpdate data if update is required)
     */
    @Throws(MobilyException::class)
    fun getAppPlatform(context: Context): JSONObject {
        val request = ApiRequest("GET", "/apps/${this.appId}/platforms/for-app/android")
        request.addParam("appVersionName", DeviceInfo.getAppVersionName(context))
        request.addParam("appVersionCode", DeviceInfo.getAppVersionCode(context))

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            if (response.status == 403) {
                Logger.e("Given AppId doesn't match the APIKey")
            } else if (response.status == 404) {
                Logger.e("App isn't configured for Android on MobilyFlow backoffice, in-app purchase will be broken.")
            } else {
                Logger.w("[getAppPlatform] API Error: ${response.string()}")
            }
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        return response.json().getJSONObject("data")
    }

    /**
     * Log user into MobilyFlow with his externalRef and return his uuid.
     * Throws on error.
     */
    @Throws(MobilyException::class)
    fun login(context: Context, externalRef: String): LoginResponse {
        val response: ApiResponse?
        try {
            val data = JSONObject()
                .put("externalRef", externalRef)
                .put("environment", environment.value)
                .put("locale", this.locale)

            val currentRegion = this.getCurrentRegion()
            if (currentRegion != null) {
                data.put("region", currentRegion)
            }

            data.put(
                "device", JSONObject()
                    .put("osVersion", DeviceInfo.getOSVersion())
                    .put("deviceModel", DeviceInfo.getDeviceModelName())
                    .put("appVersionName", DeviceInfo.getAppVersionName(context))
                    .put("appVersionCode", DeviceInfo.getAppVersionCode(context))
                    .put("sdkVersion", MOBILYFLOW_SDK_VERSION)
                    .put("installIdentifier", DeviceInfo.getInstallIdentifier(context))
                    .put("idfv", DeviceInfo.getIdfv(context))
                    .put("adid", DeviceInfo.getAdid(context))
            )

            response = this.helper.request(
                ApiRequest("POST", "/apps/${this.appId}/customers/login/android").setData(data)
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
            customer = jsonResponse.getJSONObject("customer"),
            platformOriginalTransactionIds = jsonArrayToStringArray(jsonResponse.getJSONArray("platformOriginalTransactionIds")),
            entitlements = jsonResponse.getJSONArray("entitlements"),
            haveMonitoringRequests = jsonResponse.optBoolean("haveMonitoringRequests"),
        )
    }

    @Throws(MobilyException::class)
    fun getMinimalProductForAndroidPurchase(sku: String): MinimalProductForAndroidPurchase {
        val request = ApiRequest("GET", "/apps/${this.appId}/products/minimal-product-for-android-purchase/$sku")

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            Logger.w("[getMinimalProductForAndroidPurchase] API Error: ${response.string()}")
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        val jsonResponse = response.json().getJSONObject("data")
        return MinimalProductForAndroidPurchase(
            type = MobilyProductType.parse(jsonResponse.getString("type")),
            isConsumable = jsonResponse.getBoolean("isConsumable")
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

        val request = ApiRequest("GET", "/apps/${this.appId}/products/for-app")
        request.addParam("environment", environment.value)
        request.addParam("locale", this.locale)
        request.addParam("platform", "android")

        val currentRegion = this.getCurrentRegion()
        if (currentRegion != null) {
            request.addParam("region", currentRegion)
        }

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
     * Get SubscriptionGroup in JSONArray format
     */
    @Throws(MobilyException::class)
    fun getSubscriptionGroups(identifiers: Array<String>?): JSONArray {
        if (identifiers != null && identifiers.isEmpty()) {
            return JSONArray()
        }

        val request = ApiRequest("GET", "/apps/${this.appId}/subscription-groups/for-app")
        request.addParam("environment", environment.value)
        request.addParam("locale", this.locale)
        request.addParam("platform", "android")

        val currentRegion = this.getCurrentRegion()
        if (currentRegion != null) {
            request.addParam("region", currentRegion)
        }

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
            Logger.w("[getSubscriptionGroups] API Error: ${response.string()}")
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        return response.json().getJSONArray("data")
    }

    /**
     * Get SubscriptionGroup in JSONObject format
     */
    @Throws(MobilyException::class)
    fun getSubscriptionGroupById(id: String): JSONObject {
        val request = ApiRequest("GET", "/apps/${this.appId}/subscription-groups/for-app/${id}")
        request.addParam("environment", environment.value)
        request.addParam("locale", this.locale)
        request.addParam("platform", "android")

        val currentRegion = this.getCurrentRegion()
        if (currentRegion != null) {
            request.addParam("region", currentRegion)
        }

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            Logger.w("[getSubscriptionGroupById] API Error: ${response.string()}")
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }

        return response.json().getJSONObject("data")
    }

    /**
     * Get entitlements
     */
    @Throws(MobilyException::class)
    fun getCustomerEntitlements(customerId: String): JSONArray {
        val request = ApiRequest("GET", "/apps/${this.appId}/customers/${customerId}/entitlements")
        request.addParam("locale", this.locale)
        request.addParam("platform", "android")

        val currentRegion = this.getCurrentRegion()
        if (currentRegion != null) {
            request.addParam("region", currentRegion)
        }

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

    /**
     * Get external entitlements
     */
    @Throws(MobilyException::class)
    fun getCustomerExternalEntitlements(customerId: String, transactions: Array<String>): JSONArray {
        val jsonTransactions = JSONArray()
        for (tx in transactions) {
            jsonTransactions.put(tx)
        }

        val data = JSONObject()
            .put("locale", this.locale)
            .put("transactions", jsonTransactions)
            .put("platform", "android")

        val currentRegion = this.getCurrentRegion()
        if (currentRegion != null) {
            data.put("region", currentRegion)
        }

        val request =
            ApiRequest("POST", "/apps/${this.appId}/customers/${customerId}/external-entitlements").setData(data)

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (!response.success) {
            Logger.w("[getCustomerExternalEntitlements] API Error: ${response.string()}")
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
                ApiRequest("POST", "/apps/${this.appId}/customers/mappings/android").setData(data)
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
                    "/apps/${this.appId}/customer-transfer-ownerships/request/android"
                ).setData(data)
            )
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        val jsonResponse = response.json()
        if (response.success) {
            return jsonResponse.getJSONObject("data").getString("id")
        } else {
            val errorType: MobilyTransferOwnershipException.Type? = try {
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
    @Throws(MobilyException::class, MobilyTransferOwnershipException::class)
    fun getTransferRequestStatus(requestId: String): MobilyTransferOwnershipStatus {
        val request = ApiRequest("GET", "/apps/${this.appId}/customer-transfer-ownerships/${requestId}/status")

        val response: ApiResponse?
        try {
            response = this.helper.request(request)
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.SERVER_UNAVAILABLE, e)
        }

        if (response.success) {
            val jsonResponse = response.json().getJSONObject("data")
            val statusStr = jsonResponse.getString("status").uppercase()
            if (statusStr == "ERROR") {
                throw MobilyTransferOwnershipException(MobilyTransferOwnershipException.Type.WEBHOOK_FAILED)
            }
            return MobilyTransferOwnershipStatus.parse(jsonResponse.getString("status"))
        } else {
            Logger.w("[getTransferRequestStatus] API Error: ${response.string()}")
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }
    }

    /**
     * Get webhook status from transactionID
     */
    @Throws(MobilyException::class)
    fun getWebhookResult(purchaseToken: String, transactionId: String): MobilyWebhookResult {
        val request = ApiRequest("POST", "/apps/${this.appId}/events/webhook-result/android")

        request.setData(
            JSONObject()
                .put("signedTransaction", purchaseToken)
                .put("platformTxId", transactionId)
                .put("environment", environment.value)
        )

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

        return MobilyWebhookResult(
            status = MobilyWebhookStatus.parse(jsonResponse.getString("status")),
            event = jsonResponse.optJSONObject("event"),
        )
    }

    /**
     * Upload monitoring file
     */
    @Throws(MobilyException::class)
    fun uploadMonitoring(context: Context, customerId: String?, file: File) {
        val request = ApiRequest("POST", "/apps/${this.appId}/monitoring/upload")
        request.addData("platform", "android")
        if (customerId != null) {
            request.addData("customerId", customerId)
        }
        request.addData("deviceInstallIdentifier", DeviceInfo.getInstallIdentifier(context))
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
    fun isForwardingEnable(externalRef: String?): Boolean {
        val request = ApiRequest("GET", "/apps/${this.appId}/customers/is-forwarding-enable")
        if (externalRef != null) {
            request.addParam("externalRef", externalRef)
        }
        request.addParam("environment", environment.value)
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