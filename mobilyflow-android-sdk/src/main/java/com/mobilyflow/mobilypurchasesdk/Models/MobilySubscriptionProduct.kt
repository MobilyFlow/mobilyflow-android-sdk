package com.mobilyflow.mobilypurchasesdk.Models

import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import org.json.JSONObject

class MobilySubscriptionProduct(
    val baseOffer: MobilySubscriptionOffer,
    val freeTrial: MobilySubscriptionOffer?,
    val promotionalOffers: List<MobilySubscriptionOffer>,
    val status: ProductStatus,
    val groupLevel: Int,
    val android_basePlanId: String,
    val subscriptionGroupId: String?,
    val subscriptionGroup: MobilySubscriptionGroup?,
) {
    companion object {
        internal fun parse(jsonProduct: JSONObject): MobilySubscriptionProduct {
            val baseOffer: MobilySubscriptionOffer
            var freeTrial: MobilySubscriptionOffer? = null
            val promotionalOffers = mutableListOf<MobilySubscriptionOffer>()
            var subscriptionGroup: MobilySubscriptionGroup? = null

            val sku = jsonProduct.getString("android_sku")
            val basePlanId = jsonProduct.getString("android_basePlanId")

            val jsonOffers = jsonProduct.optJSONArray("Offers")
            baseOffer = MobilySubscriptionOffer.parse(sku, basePlanId, jsonProduct, true)

            if (jsonOffers != null) {
                for (i in 0..<jsonOffers.length()) {
                    val jsonOffer = jsonOffers.getJSONObject(i)

                    val offer = MobilySubscriptionOffer.parse(sku, basePlanId, jsonOffer, false)

                    if (offer.isFreeTrial) {
                        if (freeTrial != null) {
                            Logger.w("Offer $sku/$basePlanId is incompatible with MobilyFlow (too many free trials)")
                            continue
                        }
                        freeTrial = offer
                    } else {
                        promotionalOffers.add(offer)
                    }
                }
            }

            val subscriptionGroupJson = jsonProduct.optJSONObject("SubscriptionGroup")
            if (subscriptionGroupJson != null) {
                subscriptionGroup = MobilySubscriptionGroup.parse(subscriptionGroupJson)
            }

            return MobilySubscriptionProduct(
                baseOffer = baseOffer,
                freeTrial = freeTrial,
                promotionalOffers = promotionalOffers,
                status = baseOffer.status,
                groupLevel = jsonProduct.getInt("subscriptionGroupLevel"),
                android_basePlanId = basePlanId,
                subscriptionGroupId = jsonProduct.optString("subscriptionGroupId"),
                subscriptionGroup = subscriptionGroup,
            )
        }
    }
}