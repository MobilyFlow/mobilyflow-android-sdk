package com.mobilyflow.mobilypurchasesdk.Enums


enum class MobilyWebhookStatus(val value: String) {
    PENDING("PENDING"),
    FAILED("FAILED"),
    IGNORED("IGNORED"),
    SUCCESS("SUCCESS");

    companion object {
        // TODO: Retro-compatibility fallback
        private val legacyMap: Map<String, MobilyWebhookStatus> = mapOf(
            "not-sent" to PENDING,
            "pending" to PENDING,
            "failed" to FAILED,
            "error" to FAILED,
            "ignored" to IGNORED,
            "success" to SUCCESS,
        )

        fun parse(value: String): MobilyWebhookStatus {
            for (it in entries) {
                if (it.value == value) {
                    return it
                }
            }

            // TODO: Retro-compatibility fallback
            val legacy = legacyMap[value]
            if (legacy != null) {
                return legacy
            }
            // -----------------------------

            throw IllegalArgumentException("Unknown WebhookStatus: $value")
        }
    }
}