package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyProductStatus(val value: String) {
    INVALID("INVALID"),
    UNAVAILABLE("UNAVAILABLE"),
    AVAILABLE("AVAILABLE");

    companion object {
        fun parse(value: String): MobilyProductStatus {
            for (it in entries) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown ProductStatus: $value")
        }
    }
}