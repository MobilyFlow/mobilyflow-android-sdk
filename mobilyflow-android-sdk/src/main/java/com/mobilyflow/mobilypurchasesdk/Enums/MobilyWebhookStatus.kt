package com.mobilyflow.mobilypurchasesdk.Enums


enum class MobilyWebhookStatus(val value: String) {
    PENDING("PENDING"),
    FAILED("FAILED"),
    IGNORED("IGNORED"),
    SUCCESS("SUCCESS");

    companion object {
        fun parse(value: String): MobilyWebhookStatus {
            for (it in entries) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown WebhookStatus: $value")
        }
    }
}