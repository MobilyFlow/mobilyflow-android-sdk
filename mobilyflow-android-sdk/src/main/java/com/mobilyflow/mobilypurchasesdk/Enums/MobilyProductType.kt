package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyProductType(val value: String) {
    ONE_TIME("ONE_TIME"),
    SUBSCRIPTION("SUBSCRIPTION");

    companion object {
        fun parse(value: String): MobilyProductType {
            for (it in entries) {
                if (it.value == value) {
                    return it
                }
            }

            // TODO: Retro-compatibility fallback
            if (value == "one_time") {
                return ONE_TIME
            } else if (value == "subscription") {
                return SUBSCRIPTION
            }
            // ----------------------------------

            throw IllegalArgumentException("Unknown ProductType: $value")
        }
    }
}
