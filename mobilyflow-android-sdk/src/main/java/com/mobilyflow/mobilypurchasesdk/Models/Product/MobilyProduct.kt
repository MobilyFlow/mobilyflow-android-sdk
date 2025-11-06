package com.mobilyflow.mobilypurchasesdk.Models.Product

import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyProductOfferType
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyProductStatus
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyProductType
import com.mobilyflow.mobilypurchasesdk.Enums.PeriodUnit
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseRegistry
import com.mobilyflow.mobilypurchasesdk.Utils.StorePrice
import com.mobilyflow.mobilypurchasesdk.Utils.TranslationUtils
import com.mobilyflow.mobilypurchasesdk.Utils.Utils
import kotlinx.datetime.LocalDateTime
import org.json.JSONObject


class MobilyProduct(
    val id: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val identifier: String,
    val referenceName: String,
    val externalRef: String,

    val android_sku: String,
    val android_basePlanId: String,
    val ios_sku: String?,

    val type: MobilyProductType,
    val extras: JSONObject?,

    val priceMillis: Int,
    val currencyCode: String,
    val priceFormatted: String,
    val status: MobilyProductStatus,

    val name: String,
    val description: String,

    val oneTime: MobilyOneTimeProduct?,
    val subscription: MobilySubscriptionProduct?,
) {
    class MobilyOneTimeProduct(
        val isConsumable: Boolean,
        val isMultiQuantity: Boolean,
    ) {}

    class MobilySubscriptionProduct(
        val periodCount: Int,
        val periodUnit: PeriodUnit,
        val groupLevel: Int,
        val groupId: String,

        val freeTrial: MobilySubscriptionOffer?,
        val promotionalOffers: List<MobilySubscriptionOffer>,
    ) {}

    companion object {
        internal fun parse(jsonProduct: JSONObject): MobilyProduct {
            val android_sku = jsonProduct.getString("android_sku")
            val android_basePlanId = jsonProduct.getString("android_basePlanId")
            val type = MobilyProductType.parse(jsonProduct.getString("type"))

            var status: MobilyProductStatus
            var priceMillis = 0
            var currencyCode = ""
            var priceFormatted = ""

            var oneTime: MobilyOneTimeProduct? = null
            var subscription: MobilySubscriptionProduct? = null

            val androidProduct =
                MobilyPurchaseRegistry.getAndroidProduct(android_sku)

            if (type == MobilyProductType.ONE_TIME) {
                if (androidProduct?.oneTimePurchaseOfferDetails != null) {
                    status = MobilyProductStatus.AVAILABLE

                    priceMillis = Utils.microToMillis(androidProduct.oneTimePurchaseOfferDetails!!.priceAmountMicros)
                    currencyCode = androidProduct.oneTimePurchaseOfferDetails!!.priceCurrencyCode
                    priceFormatted = androidProduct.oneTimePurchaseOfferDetails!!.formattedPrice
                } else {
                    status =
                        if (androidProduct == null) MobilyProductStatus.UNAVAILABLE else MobilyProductStatus.INVALID
                }

                oneTime = MobilyOneTimeProduct(
                    isConsumable = jsonProduct.getBoolean("isConsumable"),
                    isMultiQuantity = jsonProduct.optBoolean("isMultiQuantity"),
                )
            } else {
                val periodCount: Int
                val periodUnit: PeriodUnit
                var freeTrial: MobilySubscriptionOffer? = null
                val promotionalOffers = mutableListOf<MobilySubscriptionOffer>()

                val baseOffer = MobilyPurchaseRegistry.getAndroidOffer(android_sku, android_basePlanId, null)
                status = if (baseOffer == null) MobilyProductStatus.UNAVAILABLE else MobilyProductStatus.AVAILABLE

                if (baseOffer != null) {
                    val countPhases = baseOffer.pricingPhases.pricingPhaseList.count()
                    if (countPhases != 1) {
                        Logger.w("BaseOffer for $android_sku/$android_basePlanId is incompatible with MobilyFlow (only 1 pricingPhases allowed)")
                        status = MobilyProductStatus.INVALID
                    } else {
                        val phase = baseOffer.pricingPhases.pricingPhaseList[0]
                        if (phase.recurrenceMode == RecurrenceMode.NON_RECURRING) {
                            Logger.w("Offer $android_sku/$android_basePlanId is incompatible with MobilyFlow (NON_RECURRING phase)")
                            status = MobilyProductStatus.INVALID
                        }
                    }
                }

                if (baseOffer != null && status == MobilyProductStatus.AVAILABLE) {
                    val phase = baseOffer.pricingPhases.pricingPhaseList[0]

                    priceMillis = Utils.microToMillis(phase.priceAmountMicros)
                    currencyCode = phase.priceCurrencyCode
                    priceFormatted = phase.formattedPrice

                    val periodParsed = PeriodUnit.parseSubscriptionPeriod(phase.billingPeriod)
                    periodCount = periodParsed.first
                    periodUnit = periodParsed.second
                } else {
                    periodCount = jsonProduct.getInt("subscriptionPeriodCount")
                    periodUnit = PeriodUnit.parse(jsonProduct.getString("subscriptionPeriodUnit"))
                }

                val jsonOffers = jsonProduct.optJSONArray("Offers")

                if (jsonOffers != null) {
                    for (i in 0..<jsonOffers.length()) {
                        val jsonOffer = jsonOffers.getJSONObject(i)

                        val offer =
                            MobilySubscriptionOffer.parse(android_sku, android_basePlanId, jsonProduct, jsonOffer)

                        if (offer.type == MobilyProductOfferType.FREE_TRIAL) {
                            if (freeTrial != null) {
                                Logger.w("Offer $android_sku/$android_basePlanId/${offer.android_offerId} is incompatible with MobilyFlow (too many free trials)")
                                continue
                            }
                            freeTrial = offer
                        } else {
                            promotionalOffers.add(offer)
                        }
                    }
                }

                subscription = MobilySubscriptionProduct(
                    periodCount = periodCount,
                    periodUnit = periodUnit,
                    groupLevel = jsonProduct.getInt("subscriptionGroupLevel"),
                    groupId = jsonProduct.getString("subscriptionGroupId"),
                    freeTrial = freeTrial,
                    promotionalOffers = promotionalOffers,
                )
            }

            if (status != MobilyProductStatus.AVAILABLE) {
                val storePriceJson = jsonProduct.optJSONArray("StorePrices")
                val storePrice =
                    if ((storePriceJson?.length() ?: 0) > 0)
                        StorePrice.parse(storePriceJson!!.getJSONObject(0))
                    else null

                priceMillis = storePrice?.priceMillis ?: 0
                currencyCode = storePrice?.currency ?: ""
                priceFormatted = Utils.formatPrice(priceMillis, currencyCode)
            }

            val product = MobilyProduct(
                id = jsonProduct.getString("id"),
                createdAt = Utils.parseDate(jsonProduct.getString("createdAt")),
                updatedAt = Utils.parseDate(jsonProduct.getString("updatedAt")),
                identifier = jsonProduct.getString("identifier"),
                referenceName = jsonProduct.getString("referenceName"),
                externalRef = jsonProduct.getString("externalRef"),

                android_sku = android_sku,
                android_basePlanId = android_basePlanId,
                ios_sku = jsonProduct.optString("ios_sku"),

                type = type,
                extras = jsonProduct.optJSONObject("extras"),

                name = TranslationUtils.getTranslationValue(jsonProduct.getJSONArray("_translations"), "name")!!,
                description = TranslationUtils.getTranslationValue(
                    jsonProduct.getJSONArray("_translations"),
                    "description"
                )!!,

                priceMillis = priceMillis,
                currencyCode = currencyCode,
                priceFormatted = priceFormatted,
                status = status,

                oneTime = oneTime,
                subscription = subscription,
            )

            return product
        }
    }
}
