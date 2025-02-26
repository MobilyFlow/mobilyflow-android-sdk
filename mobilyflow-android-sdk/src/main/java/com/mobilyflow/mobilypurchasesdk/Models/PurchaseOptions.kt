package com.mobilyflow.mobilypurchasesdk.Models

class PurchaseOptions {
    var offer: MobilySubscriptionOffer? = null

    fun setOffer(offer: MobilySubscriptionOffer?): PurchaseOptions {
        this.offer = offer
        return this
    }
}