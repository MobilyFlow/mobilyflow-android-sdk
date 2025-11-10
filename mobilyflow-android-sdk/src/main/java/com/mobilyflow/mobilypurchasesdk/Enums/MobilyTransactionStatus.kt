package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyTransactionStatus(val value: String) {
    SUCCESS("success"),
    BILLING_ERROR("billing-error"),
    REFUNDED("refunded");

    companion object {
        fun parse(value: String): MobilyTransactionStatus {
            for (it in MobilyTransactionStatus.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown MobilyTransactionStatus: $value")
        }
    }
}