package com.mobilyflow.mobilypurchasesdk.Enums


enum class MobilyWebhookStatus(val value: String) {
    PENDING("pending"),
    ERROR("error"),
    SUCCESS("success");

    companion object {
        fun parse(value: String): MobilyWebhookStatus {
            for (it in MobilyWebhookStatus.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown WebhookStatus: $value")
        }
    }
}