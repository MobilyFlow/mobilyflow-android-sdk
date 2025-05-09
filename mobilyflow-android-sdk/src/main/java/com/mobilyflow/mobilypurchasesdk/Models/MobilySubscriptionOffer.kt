package com.mobilyflow.mobilypurchasesdk.Models

import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.mobilyflow.mobilypurchasesdk.Enums.PeriodUnit
import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseRegistry
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import org.json.JSONObject

class MobilySubscriptionOffer(
    val id: String?, // null for base offer
    val identifier: String?, // null for base offer
    val externalRef: String?, // null for base offer
    val name: String,
    val price: Double,
    val currencyCode: String,
    val priceFormatted: String,
    val type: String,
    val periodCount: Int, // free_trial only
    val periodUnit: PeriodUnit, // free_trial only
    val countBillingCycle: Int, // recurring only
    val android_offerId: String?, // null for base offer
    val extras: JSONObject?,
    val status: ProductStatus,
) {
    companion object {
        internal fun parse(
            sku: String,
            basePlanId: String,
            jsonOffer: JSONObject,
            isBaseOffer: Boolean
        ): MobilySubscriptionOffer {
            var id: String? = null
            var identifier: String? = null
            var externalRef: String? = null
            var name = ""
            val price: Double
            val currencyCode: String
            val priceFormatted: String
            var type = "recurring"
            val periodCount: Int
            val periodUnit: PeriodUnit
            val countBillingCycle: Int
            var android_offerId: String? = null
            var extras: JSONObject? = null
            var status: ProductStatus = ProductStatus.UNAVAILABLE

            if (!isBaseOffer) {
                id = jsonOffer.optString("id")
                identifier = jsonOffer.optString("identifier")
                externalRef = jsonOffer.optString("externalRef")
                name = jsonOffer.optString("name")
                type = jsonOffer.getString("type")
                extras = jsonOffer.optJSONObject("extras")
                android_offerId = jsonOffer.optString("android_offerId")
            }

            // 1. Validate offer & phases
            val androidOffer =
                MobilyPurchaseRegistry.getAndroidOffer(sku, basePlanId, android_offerId)

            if (androidOffer != null) {
                val countPhases = androidOffer.pricingPhases.pricingPhaseList.count()
                if ((isBaseOffer && countPhases != 1) || (countPhases <= 0 || countPhases > 2)) {
                    Logger.w("Offer $sku/$basePlanId/${android_offerId ?: "null"} is incompatible with MobilyFlow (bad pricingPhases length ${countPhases})")
                    status = ProductStatus.INVALID
                }
                if (type == "free_trial" && androidOffer.pricingPhases.pricingPhaseList[0].priceAmountMicros > 0) {
                    Logger.w("Offer $sku/$basePlanId/${android_offerId ?: "null"} should be a free trial")
                    status = ProductStatus.INVALID
                }
                for (phase in androidOffer.pricingPhases.pricingPhaseList) {
                    if (phase.recurrenceMode == RecurrenceMode.NON_RECURRING) {
                        Logger.w("Offer $sku/${basePlanId}/${android_offerId ?: "null"} is incompatible with MobilyFlow (NON_RECURRING phase)")
                        status = ProductStatus.INVALID
                        continue
                    }
                }
            }

            // 2. Populate
            if (androidOffer == null || status == ProductStatus.INVALID) {
                price = jsonOffer.optDouble("defaultPrice", 0.0)
                currencyCode = jsonOffer.optString("defaultCurrencyCode", "")
                priceFormatted = Utils.formatPrice(price, currencyCode)

                // If isBaseOffer, jsonOffer is the jsonProduct
                if (isBaseOffer) {
                    periodCount = jsonOffer.getInt("subscriptionPeriodCount")
                    periodUnit = PeriodUnit.valueOf(jsonOffer.getString("subscriptionPeriodUnit").uppercase())
                    countBillingCycle = 0
                } else if (type == "free_trial") {
                    periodCount = jsonOffer.getInt("offerPeriodCount")
                    periodUnit = PeriodUnit.valueOf(jsonOffer.getString("offerPeriodUnit").uppercase())
                    countBillingCycle = 0
                } else {
                    periodCount = 0
                    periodUnit = PeriodUnit.WEEK
                    countBillingCycle = jsonOffer.getInt("offerCountBillingCycle")
                }
            } else {
                val phase = androidOffer.pricingPhases.pricingPhaseList[0]

                status = ProductStatus.AVAILABLE
                price = Utils.microToDouble(phase.priceAmountMicros)
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
                name = name,
                price = price,
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