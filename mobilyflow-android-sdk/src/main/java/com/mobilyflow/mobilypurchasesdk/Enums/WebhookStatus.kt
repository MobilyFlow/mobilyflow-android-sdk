package com.mobilyflow.mobilypurchasesdk.Enums


enum class WebhookStatus(val value: String) {
    PENDING("pending"),
    ERROR("error"),
    SUCCESS("success");

    companion object {
        fun parse(value: String): WebhookStatus {
            for (it in WebhookStatus.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown WebhookStatus: $value")
        }
    }
}