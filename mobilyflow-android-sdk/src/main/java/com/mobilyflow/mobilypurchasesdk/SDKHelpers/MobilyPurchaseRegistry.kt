package com.mobilyflow.mobilypurchasesdk.SDKHelpers

import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.ProductDetails

abstract class MobilyPurchaseRegistry {
    companion object {
        private val skuToProduct = mutableMapOf<String, ProductRegistryItem>()

        private class ProductRegistryItem(
            val product: ProductDetails?,
            var basePlans: MutableMap<String, BasePlanRegistryItem>?,
        ) {}

        private class BasePlanRegistryItem(
            var baseOffer: ProductDetails.SubscriptionOfferDetails?,
            var offers: MutableMap<String, ProductDetails.SubscriptionOfferDetails>?,
        ) {}

        fun registerAndroidProducts(products: List<ProductDetails>) {
            for (product in products) {
                registerAndroidProduct(product)
            }
        }

        fun registerAndroidProduct(product: ProductDetails) {
            val sku = product.productId

            // 1. Ensure product exists in skuToProduct
            if (!skuToProduct.containsKey(sku)) {
                skuToProduct[sku] = ProductRegistryItem(product, null)
            }

            // 2. For sub, ensure basePlan map exists
            if (product.productType == ProductType.SUBS) {
                if (skuToProduct[sku]!!.basePlans == null) {
                    skuToProduct[sku]!!.basePlans = mutableMapOf()
                }

                product.subscriptionOfferDetails!!.forEach { offer ->
                    // 3. For each basePlan, ensure it exists
                    if (!skuToProduct[sku]!!.basePlans!!.containsKey(offer.basePlanId)) {
                        skuToProduct[sku]!!.basePlans!![offer.basePlanId] =
                            BasePlanRegistryItem(null, mutableMapOf())
                    }

                    if (offer.offerId == null) {
                        skuToProduct[sku]!!.basePlans!![offer.basePlanId]!!.baseOffer = offer
                    } else {
                        skuToProduct[sku]!!.basePlans!![offer.basePlanId]!!.offers!![offer.offerId!!] =
                            offer
                    }
                }
            }
        }

        /**
         * Get ProductDetail with SKU
         */
        fun getAndroidProduct(sku: String): ProductDetails? {
            return skuToProduct[sku]?.product
        }

        /**
         * Return SubscriptionOfferDetails (baseOffer if offerId is null)
         */
        fun getAndroidOffer(
            sku: String,
            basePlanId: String,
            offerId: String?
        ): ProductDetails.SubscriptionOfferDetails? {
            return if (offerId == null) {
                skuToProduct[sku]?.basePlans?.get(basePlanId)?.baseOffer
            } else {
                skuToProduct[sku]?.basePlans?.get(basePlanId)?.offers?.get(offerId)
            }
        }
    }
}
