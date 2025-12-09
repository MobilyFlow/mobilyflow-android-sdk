package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyProductOfferPricingMode(val value: String) {
    FREE_TRIAL("FREE_TRIAL"),
    RECURRING("RECURRING");

    companion object {
        fun parse(value: String): MobilyProductOfferPricingMode {
            for (it in MobilyProductOfferPricingMode.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown ProductOfferType: $value")
        }
    }
}
