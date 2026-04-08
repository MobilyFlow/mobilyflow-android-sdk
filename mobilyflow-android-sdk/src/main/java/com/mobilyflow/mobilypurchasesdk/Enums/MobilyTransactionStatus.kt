package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyTransactionStatus(val value: String) {
    SUCCESS("SUCCESS"),
    BILLING_ERROR("BILLING_ERROR"),
    REFUNDED("REFUNDED");

    companion object {
        private val legacyMap: Map<String, MobilyTransactionStatus> = mapOf(
            "success" to SUCCESS,
            "billing-error" to BILLING_ERROR,
            "refunded" to REFUNDED,
        )

        fun parse(value: String): MobilyTransactionStatus {
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
            // ----------------------------------

            throw IllegalArgumentException("Unknown MobilyTransactionStatus: $value")
        }
    }
}