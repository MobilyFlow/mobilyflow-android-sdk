package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyProductType(val value: String) {
    ONE_TIME("ONE_TIME"),
    SUBSCRIPTION("SUBSCRIPTION");

    companion object {
        private val legacyMap: Map<String, MobilyProductType> = mapOf(
            "one_time" to ONE_TIME,
            "subscription" to SUBSCRIPTION,
        )

        fun parse(value: String): MobilyProductType {
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

            throw IllegalArgumentException("Unknown ProductType: $value")
        }
    }
}
