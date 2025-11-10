package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyProductStatus(val value: String) {
    INVALID("invalid"),
    UNAVAILABLE("unavailable"),
    AVAILABLE("available");

    companion object {
        fun parse(value: String): MobilyProductStatus {
            for (it in MobilyProductStatus.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown ProductStatus: $value")
        }
    }
}