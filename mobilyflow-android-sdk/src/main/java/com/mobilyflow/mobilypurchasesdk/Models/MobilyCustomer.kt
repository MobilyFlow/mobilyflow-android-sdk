package com.mobilyflow.mobilypurchasesdk.Models

import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject

class MobilyCustomer(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val externalRef: String?,
    var isForwardingEnable: Boolean,
) {
    companion object {
        internal fun parse(
            jsonCustomer: JSONObject,
            isForwardingEnable: Boolean
        ): MobilyCustomer {
            return MobilyCustomer(
                id = jsonCustomer.getString("id"),
                createdAt = Utils.parseDate(jsonCustomer.getString("createdAt")),
                updatedAt = Utils.parseDate(jsonCustomer.getString("updatedAt")),
                externalRef = jsonCustomer.getString("externalRef"),
                isForwardingEnable = isForwardingEnable
            )
        }
    }
}