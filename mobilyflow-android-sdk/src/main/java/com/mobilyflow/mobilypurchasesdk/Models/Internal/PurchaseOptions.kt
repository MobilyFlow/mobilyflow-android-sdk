package com.mobilyflow.mobilypurchasesdk.Models.Internal

import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilySubscriptionOffer

class PurchaseOptions(
    var offer: MobilySubscriptionOffer? = null
) {
    fun setOffer(offer: MobilySubscriptionOffer?): PurchaseOptions {
        this.offer = offer
        return this
    }
}