package com.mobilyflow.mobilypurchasesdk.Models.Internal

import com.mobilyflow.mobilypurchasesdk.Enums.MobilyWebhookStatus
import org.json.JSONObject

class MobilyWebhookResult(
    val status: MobilyWebhookStatus,
    val event: JSONObject?
) {}