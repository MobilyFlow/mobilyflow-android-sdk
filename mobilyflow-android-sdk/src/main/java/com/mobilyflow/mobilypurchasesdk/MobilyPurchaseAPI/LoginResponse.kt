package com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI

import org.json.JSONArray
import org.json.JSONObject

class LoginResponse(
    val customer: JSONObject,
    val entitlements: JSONArray,
    val platformOriginalTransactionIds: Array<String>,
    val isForwardingEnable: Boolean,
    val haveMonitoringRequests: Boolean,
) {}