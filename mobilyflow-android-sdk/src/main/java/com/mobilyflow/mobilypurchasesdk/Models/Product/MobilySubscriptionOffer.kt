package com.mobilyflow.mobilypurchasesdk.Models.Product

import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.mobilyflow.mobilypurchasesdk.Enums.PeriodUnit
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyProductOfferType
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyProductStatus
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseRegistry
import com.mobilyflow.mobilypurchasesdk.Utils.StorePrice
import com.mobilyflow.mobilypurchasesdk.Utils.TranslationUtils
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import org.json.JSONObject

class MobilySubscriptionOffer(
    val id: String,
    val identifier: String,
    val externalRef: String,
    val referenceName: String,
    val priceMillis: Int,
    val currencyCode: String,
    val priceFormatted: String,
    val type: MobilyProductOfferType,
    val periodCount: Int,
    val periodUnit: PeriodUnit,
    val countBillingCycle: Int,
    val android_offerId: String,
    val extras: JSONObject?,
    val status: MobilyProductStatus,

    val name: String,
) {
    companion object {
        internal fun parse(
            sku: String,
            basePlanId: String,
            jsonProduct: JSONObject,
            jsonOffer: JSONObject,
        ): MobilySubscriptionOffer {
            val id = jsonOffer.getString("id")
            val identifier = jsonOffer.getString("identifier")
            val externalRef = jsonOffer.optString("externalRef")
            val referenceName = jsonOffer.optString("referenceName")
            val name = TranslationUtils.getTranslationValue(jsonOffer.getJSONArray("_translations"), "name")!!
            val type = MobilyProductOfferType.parse(jsonOffer.getString("type"))
            val extras = jsonOffer.optJSONObject("extras")
            val android_offerId = jsonOffer.getString("android_offerId")

            val priceMillis: Int
            val currencyCode: String
            val priceFormatted: String
            val periodCount: Int
            val periodUnit: PeriodUnit
            val countBillingCycle: Int
            var status: MobilyProductStatus = MobilyProductStatus.UNAVAILABLE

            // 1. Validate offer & phases
            val androidOffer = MobilyPurchaseRegistry.getAndroidOffer(sku, basePlanId, android_offerId)

            if (androidOffer != null) {
                val countPhases = androidOffer.pricingPhases.pricingPhaseList.count()
                if (countPhases != 2) {
                    Logger.w("Offer $sku/$basePlanId/${android_offerId} is incompatible with MobilyFlow (only 2 pricingPhases allowed)")
                    status = MobilyProductStatus.INVALID
                } else {
                    if (type == MobilyProductOfferType.FREE_TRIAL && androidOffer.pricingPhases.pricingPhaseList[0].priceAmountMicros > 0) {
                        Logger.w("Offer $sku/$basePlanId/${android_offerId ?: "null"} should be a free trial")
                        status = MobilyProductStatus.INVALID
                    }
                    for (phase in androidOffer.pricingPhases.pricingPhaseList) {
                        if (phase.recurrenceMode == RecurrenceMode.NON_RECURRING) {
                            Logger.w("Offer $sku/${basePlanId}/${android_offerId ?: "null"} is incompatible with MobilyFlow (NON_RECURRING phase)")
                            status = MobilyProductStatus.INVALID
                            continue
                        }
                    }
                }
            }

            // 2. Populate
            if (androidOffer == null || status == MobilyProductStatus.INVALID) {
                // Promotional offer but unavailable
                val storePriceJson = jsonOffer.optJSONArray("StorePrices")
                val storePrice =
                    if ((storePriceJson?.length() ?: 0) > 0)
                        StorePrice.parse(storePriceJson!!.getJSONObject(0))
                    else null

                priceMillis = storePrice?.priceMillis ?: 0
                currencyCode = storePrice?.currency ?: ""

                priceFormatted = Utils.formatPrice(priceMillis, currencyCode)

                if (type == MobilyProductOfferType.FREE_TRIAL) {
                    periodCount = jsonOffer.getInt("offerPeriodCount")
                    periodUnit = PeriodUnit.parse(jsonOffer.getString("offerPeriodUnit"))
                    countBillingCycle = 1
                } else {
                    countBillingCycle = jsonOffer.getInt("offerCountBillingCycle")

                    // Inherit from baseOffer
                    periodCount = jsonProduct.getInt("subscriptionPeriodCount")
                    periodUnit = PeriodUnit.parse(jsonProduct.getString("subscriptionPeriodUnit"))
                }
            } else {
                val phase = androidOffer.pricingPhases.pricingPhaseList[0]

                status = MobilyProductStatus.AVAILABLE
                priceMillis = Utils.microToMillis(phase.priceAmountMicros)
                currencyCode = phase.priceCurrencyCode
                priceFormatted = phase.formattedPrice

                val periodParsed = PeriodUnit.parseSubscriptionPeriod(phase.billingPeriod)
                periodCount = periodParsed.first
                periodUnit = periodParsed.second
                countBillingCycle = phase.billingCycleCount
            }

            return MobilySubscriptionOffer(
                id = id,
                identifier = identifier,
                externalRef = externalRef,
                referenceName = referenceName,
                name = name,
                priceMillis = priceMillis,
                currencyCode = currencyCode,
                priceFormatted = priceFormatted,
                type = type,
                periodCount = periodCount,
                periodUnit = periodUnit,
                countBillingCycle = countBillingCycle,
                android_offerId = android_offerId,
                extras = extras,
                status = status,
            )
        }
    }
}