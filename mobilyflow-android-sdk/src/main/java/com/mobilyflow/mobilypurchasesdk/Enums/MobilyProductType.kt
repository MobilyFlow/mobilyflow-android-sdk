package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyProductType(val value: String) {
    ONE_TIME("one_time"),
    SUBSCRIPTION("subscription");

    companion object {
        fun parse(value: String): MobilyProductType {
            for (it in MobilyProductType.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown ProductType: $value")
        }
    }
}
