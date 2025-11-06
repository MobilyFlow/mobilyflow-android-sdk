package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyEnvironment(val value: String) {
    DEVELOPMENT("development"),
    STAGING("staging"),
    PRODUCTION("production");

    companion object {
        fun parse(value: String): MobilyEnvironment {
            for (it in MobilyEnvironment.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown MobilyEnvironment: $value")
        }
    }
}