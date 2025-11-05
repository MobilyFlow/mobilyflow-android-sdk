package com.mobilyflow.mobilypurchasesdk.Models

import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.mobilyflow.mobilypurchasesdk.Enums.PeriodUnit
import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseRegistry
import com.mobilyflow.mobilypurchasesdk.Utils.StorePrice
import com.mobilyflow.mobilypurchasesdk.Utils.TranslationUtils
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import org.json.JSONObject

class MobilySubscriptionOffer(
    val id: String?, // null for base offer
    val identifier: String?, // null for base offer
    val externalRef: String?, // null for base offer
    val referenceName: String?,
    val name: String,
    val priceMillis: Int,
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
            jsonBase: JSONObject,
            jsonOffer: JSONObject?,
        ): MobilySubscriptionOffer {
            var id: String? = null
            var identifier: String? = null
            var externalRef: String? = null
            var referenceName: String? = null
            var name = ""
            val priceMillis: Int
            val currencyCode: String
            val priceFormatted: String
            var type = "recurring"
            val periodCount: Int
            val periodUnit: PeriodUnit
            val countBillingCycle: Int
            var android_offerId: String? = null
            var extras: JSONObject? = null
            var status: ProductStatus = ProductStatus.UNAVAILABLE

            if (jsonOffer != null) {
                id = jsonOffer.optString("id")
                identifier = jsonOffer.optString("identifier")
                externalRef = jsonOffer.optString("externalRef")
                referenceName = jsonOffer.optString("referenceName")
                name = TranslationUtils.getTranslationValue(jsonOffer.getJSONArray("_translations"), "name")!!
                type = jsonOffer.getString("type")
                extras = jsonOffer.optJSONObject("extras")
                android_offerId = jsonOffer.optString("android_offerId")
            }

            // 1. Validate offer & phases
            val androidOffer =
                MobilyPurchaseRegistry.getAndroidOffer(sku, basePlanId, android_offerId)

            if (androidOffer != null) {
                val countPhases = androidOffer.pricingPhases.pricingPhaseList.count()
                if ((jsonOffer == null && countPhases != 1) || (countPhases <= 0 || countPhases > 2)) {
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

                if (jsonOffer == null) {
                    // Base Offer but unavailable
                    val storePriceJson = jsonBase.optJSONArray("StorePrices")
                    val storePrice =
                        if ((storePriceJson?.length() ?: 0) > 0)
                            StorePrice.parse(storePriceJson!!.getJSONObject(0))
                        else null

                    priceMillis = storePrice?.priceMillis ?: 0
                    currencyCode = storePrice?.currency ?: ""

                    priceFormatted = Utils.formatPrice(priceMillis, currencyCode)

                    periodCount = jsonBase.getInt("subscriptionPeriodCount")
                    periodUnit = PeriodUnit.valueOf(jsonBase.getString("subscriptionPeriodUnit").uppercase())
                    countBillingCycle = 0
                } else {
                    // Promotional offer but unavailable
                    val storePriceJson = jsonOffer.optJSONArray("StorePrices")
                    val storePrice =
                        if ((storePriceJson?.length() ?: 0) > 0)
                            StorePrice.parse(storePriceJson!!.getJSONObject(0))
                        else null

                    priceMillis = storePrice?.priceMillis ?: 0
                    currencyCode = storePrice?.currency ?: ""

                    priceFormatted = Utils.formatPrice(priceMillis, currencyCode)

                    if (type == "free_trial") {
                        periodCount = jsonOffer.getInt("offerPeriodCount")
                        periodUnit = PeriodUnit.valueOf(jsonOffer.getString("offerPeriodUnit").uppercase())
                        countBillingCycle = 1
                    } else {
                        countBillingCycle = jsonOffer.getInt("offerCountBillingCycle")

                        // Inherit from baseOffer
                        periodCount = jsonBase.getInt("subscriptionPeriodCount")
                        periodUnit = PeriodUnit.valueOf(jsonBase.getString("subscriptionPeriodUnit").uppercase())
                    }
                }
            } else {
                val phase = androidOffer.pricingPhases.pricingPhaseList[0]

                status = ProductStatus.AVAILABLE
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