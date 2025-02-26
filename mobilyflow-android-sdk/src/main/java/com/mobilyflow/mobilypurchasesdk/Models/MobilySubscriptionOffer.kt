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
    val name: String,
    val price: Double,
    val currencyCode: String,
    val priceFormatted: String,
    val isFreeTrial: Boolean,
    val periodCount: Int,
    val periodUnit: PeriodUnit,
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
            var name = ""
            val price: Double
            val currencyCode: String
            val priceFormatted: String
            var isFreeTrial = false
            val periodCount: Int
            val periodUnit: PeriodUnit
            var android_offerId: String? = null
            var extras: JSONObject? = null
            var status: ProductStatus = ProductStatus.UNAVAILABLE

            if (!isBaseOffer) {
                id = jsonOffer.optString("id")
                name = jsonOffer.optString("name")
                isFreeTrial = jsonOffer.optBoolean("isFreeTrial")
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
                if (isFreeTrial && androidOffer.pricingPhases.pricingPhaseList[0].priceAmountMicros > 0) {
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
                val periodPrefix = if (isBaseOffer) "subscription" else "offer"
                periodCount = jsonOffer.getInt(periodPrefix + "PeriodCount")
                periodUnit = PeriodUnit.valueOf(
                    jsonOffer.getString(periodPrefix + "PeriodUnit").uppercase()
                )
            } else {
                val phase = androidOffer.pricingPhases.pricingPhaseList[0]

                status = ProductStatus.AVAILABLE
                price = Utils.microToDouble(phase.priceAmountMicros)
                currencyCode = phase.priceCurrencyCode
                priceFormatted = phase.formattedPrice

                val periodParsed = PeriodUnit.parseSubscriptionPeriod(phase.billingPeriod)
                periodCount = periodParsed.first
                periodUnit = periodParsed.second
            }

            return MobilySubscriptionOffer(
                id = id,
                name = name,
                price = price,
                currencyCode = currencyCode,
                priceFormatted = priceFormatted,
                isFreeTrial = isFreeTrial,
                periodCount = periodCount,
                periodUnit = periodUnit,
                android_offerId = android_offerId,
                extras = extras,
                status = status,
            )
        }
    }
}