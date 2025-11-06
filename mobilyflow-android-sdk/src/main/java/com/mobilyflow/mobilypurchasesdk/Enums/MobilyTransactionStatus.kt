package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyTransactionStatus(val value: Int) {
    SUCCESS(0),
    BILLING_ERROR(1),
    REFUNDED(2);

    companion object {
        fun parse(strValue: String): MobilyTransactionStatus {
            return when (strValue) {
                "success" -> SUCCESS
                "billing-error" -> BILLING_ERROR
                "refunded" -> REFUNDED
                else -> throw IllegalArgumentException("Unknown Event: ${strValue}")
            }
        }
    }
}