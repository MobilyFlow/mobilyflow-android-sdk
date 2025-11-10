package com.mobilyflow.mobilypurchasesdk.Models

import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject

class MobilyCustomer(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val externalRef: String,
    var forwardNotificationEnable: Boolean,
) {
    companion object {
        internal fun parse(json: JSONObject): MobilyCustomer {
            return MobilyCustomer(
                id = json.getString("id"),
                createdAt = Utils.parseDate(json.getString("createdAt")),
                updatedAt = Utils.parseDate(json.getString("updatedAt")),
                externalRef = json.getString("externalRef"),
                forwardNotificationEnable = json.getBoolean("forwardNotificationEnable")
            )
        }
    }
}