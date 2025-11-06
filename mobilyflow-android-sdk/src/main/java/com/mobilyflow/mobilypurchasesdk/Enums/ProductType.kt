package com.mobilyflow.mobilypurchasesdk.Enums

enum class ProductType(val value: String) {
    ONE_TIME("one_time"),
    SUBSCRIPTION("subscription");

    companion object {
        fun parse(value: String): ProductType {
            for (it in ProductType.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown ProductType: $value")
        }
    }
}
