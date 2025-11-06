package com.mobilyflow.mobilypurchasesdk.Models.Product

import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.mobilyflow.mobilypurchasesdk.Enums.PeriodUnit
import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
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

    val type: ProductType,
    val extras: JSONObject?,

    val priceMillis: Int,
    val currencyCode: String,
    val priceFormatted: String,
    val status: ProductStatus,

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
            val type = ProductType.valueOf(jsonProduct.getString("type").uppercase())

            var status: ProductStatus
            var priceMillis = 0
            var currencyCode = ""
            var priceFormatted = ""

            var oneTime: MobilyOneTimeProduct? = null
            var subscription: MobilySubscriptionProduct? = null

            val androidProduct =
                MobilyPurchaseRegistry.getAndroidProduct(jsonProduct.getString("android_sku"))

            /*
            val storePriceJson = jsonProduct.optJSONArray("StorePrices")
                    val storePrice =
                        if ((storePriceJson?.length() ?: 0) > 0)
                            StorePrice.parse(storePriceJson!!.getJSONObject(0))
                        else null

                    priceMillis = storePrice?.priceMillis ?: 0
                    currencyCode = storePrice?.currency ?: ""

                    priceFormatted = Utils.formatPrice(priceMillis, currencyCode)
             */

            if (type == ProductType.ONE_TIME) {
                if (androidProduct?.oneTimePurchaseOfferDetails != null) {
                    status = ProductStatus.AVAILABLE

                    priceMillis = Utils.microToMillis(androidProduct.oneTimePurchaseOfferDetails!!.priceAmountMicros)
                    currencyCode = androidProduct.oneTimePurchaseOfferDetails!!.priceCurrencyCode
                    priceFormatted = androidProduct.oneTimePurchaseOfferDetails!!.formattedPrice
                } else {
                    status = if (androidProduct == null) ProductStatus.UNAVAILABLE else ProductStatus.INVALID
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
                status = if (baseOffer == null) ProductStatus.UNAVAILABLE else ProductStatus.AVAILABLE

                if (baseOffer != null) {
                    val countPhases = baseOffer.pricingPhases.pricingPhaseList.count()
                    if (countPhases != 1) {
                        Logger.w("BaseOffer for $android_sku/$android_basePlanId is incompatible with MobilyFlow (only 1 pricingPhases allowed)")
                        status = ProductStatus.INVALID
                    } else {
                        val phase = baseOffer.pricingPhases.pricingPhaseList[0]
                        if (phase.recurrenceMode == RecurrenceMode.NON_RECURRING) {
                            Logger.w("Offer $android_sku/$android_basePlanId is incompatible with MobilyFlow (NON_RECURRING phase)")
                            status = ProductStatus.INVALID
                        }
                    }
                }

                if (baseOffer != null && status == ProductStatus.AVAILABLE) {
                    val phase = baseOffer.pricingPhases.pricingPhaseList[0]

                    status = ProductStatus.AVAILABLE
                    priceMillis = Utils.microToMillis(phase.priceAmountMicros)
                    currencyCode = phase.priceCurrencyCode
                    priceFormatted = phase.formattedPrice

                    val periodParsed = PeriodUnit.parseSubscriptionPeriod(phase.billingPeriod)
                    periodCount = periodParsed.first
                    periodUnit = periodParsed.second
                } else {
                    periodCount = jsonProduct.getInt("subscriptionPeriodCount")
                    periodUnit = PeriodUnit.valueOf(jsonProduct.getString("subscriptionPeriodUnit").uppercase())
                }

                val jsonOffers = jsonProduct.optJSONArray("Offers")

                if (jsonOffers != null) {
                    for (i in 0..<jsonOffers.length()) {
                        val jsonOffer = jsonOffers.getJSONObject(i)

                        val offer =
                            MobilySubscriptionOffer.parse(android_sku, android_basePlanId, jsonProduct, jsonOffer)

                        if (offer.type == "free_trial") {
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

            if (status != ProductStatus.AVAILABLE) {
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