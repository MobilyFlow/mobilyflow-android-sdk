package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyProductOfferType(val value: String) {
    INTRODUCTORY("INTRODUCTORY"),
    DEVELOPER_DETERMINED("DEVELOPER_DETERMINED");

    companion object {
        fun parse(value: String): MobilyProductOfferType {
            for (it in entries) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown ProductOfferType: $value")
        }
    }
}
