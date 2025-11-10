package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyProductOfferType(val value: String) {
    FREE_TRIAL("free_trial"),
    RECURRING("recurring");

    companion object {
        fun parse(value: String): MobilyProductOfferType {
            for (it in MobilyProductOfferType.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown ProductOfferType: $value")
        }
    }
}
