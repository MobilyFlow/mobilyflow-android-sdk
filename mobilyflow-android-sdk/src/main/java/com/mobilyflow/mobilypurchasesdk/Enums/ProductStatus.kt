package com.mobilyflow.mobilypurchasesdk.Enums

enum class ProductStatus(val value: String) {
    INVALID("invalid"),
    UNAVAILABLE("unavailable"),
    AVAILABLE("available");

    companion object {
        fun parse(value: String): ProductStatus {
            for (it in ProductStatus.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown ProductStatus: $value")
        }
    }
}