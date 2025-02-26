package com.mobilyflow.mobilypurchasesdk.Models

import org.json.JSONObject

class MobilySubscriptionGroup(
    val id: String,
    val identifier: String,
    val name: String,
    val description: String,
    val extras: JSONObject?,
    var products: List<MobilyProduct>?
) {
    companion object {
        internal fun parse(
            jsonGroup: JSONObject,
        ): MobilySubscriptionGroup {
            return MobilySubscriptionGroup(
                id = jsonGroup.getString("id"),
                identifier = jsonGroup.getString("identifier"),
                name = jsonGroup.optString("name"),
                description = jsonGroup.optString("description") ?: "",
                extras = jsonGroup.optJSONObject("extras"),
                products = null,
            )
        }
    }

    fun clone(): MobilySubscriptionGroup {
        return MobilySubscriptionGroup(
            id = this.id,
            identifier = this.identifier,
            name = this.name,
            description = this.description,
            extras = this.extras,
            products = null,
        )
    }
}