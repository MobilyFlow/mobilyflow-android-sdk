package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyTransactionStatus(val value: String) {
    SUCCESS("SUCCESS"),
    BILLING_ERROR("BILLING_ERROR"),
    REFUNDED("REFUNDED");

    companion object {
        fun parse(value: String): MobilyTransactionStatus {
            for (it in entries) {
                if (it.value == value) {
                    return it
                }
            }

            // TODO: Retro-compatibility fallback
            if (value == "success") {
                return SUCCESS
            } else if (value == "billing-error") {
                return BILLING_ERROR
            } else if (value == "refunded") {
                return REFUNDED
            }
            // ----------------------------------

            throw IllegalArgumentException("Unknown MobilyTransactionStatus: $value")
        }
    }
}