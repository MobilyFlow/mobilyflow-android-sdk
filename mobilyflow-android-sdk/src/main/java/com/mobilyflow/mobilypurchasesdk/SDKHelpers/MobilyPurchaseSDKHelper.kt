package com.mobilyflow.mobilypurchasesdk.SDKHelpers

import com.android.billingclient.api.BillingFlowParams
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyPurchaseException
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MapTransactionItem
import com.mobilyflow.mobilypurchasesdk.Models.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Models.PurchaseOptions
import com.mobilyflow.mobilypurchasesdk.Utils.Utils.Companion.sha256

class MobilyPurchaseSDKHelper() {
    companion object {
        fun getTransactionsToMap(
            knownPurchaseTokenHashes: Array<String>,
            purchases: List<BillingClientWrapper.PurchaseWithType>
        ): Array<MapTransactionItem> {
            val knowHashes = knownPurchaseTokenHashes.toCollection(ArrayList())
            val transactionToMap = arrayListOf<MapTransactionItem>()

            for (it in purchases) {
                val purchaseTokenHash = sha256(it.purchase.purchaseToken)

                if (!knowHashes.contains(purchaseTokenHash)) {
                    transactionToMap.add(
                        MapTransactionItem(
                            it.purchase.products[0],
                            it.purchase.purchaseToken,
                            it.type,
                        )
                    )
                    knowHashes.add(purchaseTokenHash)
                }
            }

            return transactionToMap.toTypedArray()
        }

        fun getAllPurchaseTokens(purchases: List<BillingClientWrapper.PurchaseWithType>): Array<String> {
            val transactionsToClaim = arrayListOf<String>()

            for (it in purchases) {
                if (it.purchase.isAcknowledged) {
                    if (!transactionsToClaim.contains(it.purchase.purchaseToken)) {
                        transactionsToClaim.add(it.purchase.purchaseToken)
                    }
                }
            }

            return transactionsToClaim.toTypedArray()
        }

        fun createBillingFlowParams(
            syncer: MobilyPurchaseSDKSyncer,
            customerId: String,
            product: MobilyProduct,
            options: PurchaseOptions? = null,
        ): BillingFlowParams {
            val androidProduct = MobilyPurchaseRegistry.getAndroidProduct(product.android_sku)
            val androidOffer = if (product.type == ProductType.SUBSCRIPTION) {
                MobilyPurchaseRegistry.getAndroidOffer(
                    product.android_sku,
                    product.subscriptionProduct!!.android_basePlanId,
                    if (options?.offer == null && product.subscriptionProduct.freeTrial?.status == ProductStatus.AVAILABLE)
                        product.subscriptionProduct.freeTrial.android_offerId
                    else
                        options?.offer?.android_offerId
                )
            } else {
                null
            }

            if (androidProduct == null || (product.type == ProductType.SUBSCRIPTION && androidOffer == null)) {
                throw MobilyPurchaseException(MobilyPurchaseException.Type.PRODUCT_UNAVAILABLE)
            }

            val productDetailsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(androidProduct)

            if (androidOffer != null) {
                productDetailsBuilder.setOfferToken(androidOffer.offerToken)
            }

            val builder =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsBuilder.build()))

            builder.setObfuscatedAccountId(customerId)

            // Manage already purchased
            if (product.type == ProductType.ONE_TIME) {
                if (!product.oneTimeProduct!!.isConsumable) {
                    val entitlement = syncer.getEntitlement(product.id)
                    if (entitlement != null) {
                        throw MobilyPurchaseException(MobilyPurchaseException.Type.ALREADY_PURCHASED)
                    } else {
                        val storeAccountTransaction = syncer.getStoreAccountTransaction(product.android_sku)

                        if (storeAccountTransaction != null) {
                            // Another customer is already entitled to this product on the same store account
                            throw MobilyPurchaseException(MobilyPurchaseException.Type.STORE_ACCOUNT_ALREADY_HAVE_PURCHASE)
                        }
                    }
                }
            } else {
                val entitlement =
                    syncer.getEntitlementForSubscription(product.subscriptionProduct!!.subscriptionGroupId)

                if (entitlement != null) {
                    if (!entitlement.subscription!!.isManagedByThisStoreAccount) {
                        throw MobilyPurchaseException(MobilyPurchaseException.Type.NOT_MANAGED_BY_THIS_STORE_ACCOUNT)
                    }

                    // If auto-renew is disabled, allow re-purchase in app
                    if (entitlement.subscription.autoRenewEnable) {
                        val currentRenewProduct = entitlement.subscription.renewProduct ?: entitlement.product
                        val currentRenewSku = currentRenewProduct.android_sku
                        val currentRenewBasePlan = currentRenewProduct.subscriptionProduct!!.android_basePlanId

                        if (currentRenewSku == product.android_sku && currentRenewBasePlan == product.subscriptionProduct.android_basePlanId) {
                            if (
                                entitlement.product.android_sku == product.android_sku &&
                                entitlement.product.subscriptionProduct!!.android_basePlanId == product.subscriptionProduct.android_basePlanId
                            ) {
                                throw MobilyPurchaseException(MobilyPurchaseException.Type.ALREADY_PURCHASED)
                            } else {
                                throw MobilyPurchaseException(MobilyPurchaseException.Type.RENEW_ALREADY_ON_THIS_PLAN)
                            }
                        }
                    }

                    builder.setSubscriptionUpdateParams(
                        BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                            .setOldPurchaseToken(entitlement.subscription.purchaseToken!!)
                            // https://developer.android.com/reference/com/android/billingclient/api/BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
                            .setSubscriptionReplacementMode(
                                if (entitlement.product.android_sku == product.android_sku) {
                                    // Always DOWNGRADE when same SKU, officially, this credit the subscription directly, but only rebuy at the end of actual period
                                    // This is the only ReplacementMode that work when SKU is the same
                                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION
                                } else if (entitlement.product.subscriptionProduct!!.groupLevel > product.subscriptionProduct.groupLevel) {
                                    // When UPGRADE, Charge immediately, remaining time is prorated and added to the subscription
                                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
                                } else {
                                    // DOWNGRADE: Charge at next billing date
                                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED
                                }
                            )
                            .build()
                    )
                } else {
                    val storeAccountTransaction = syncer.getStoreAccountTransaction(product.android_sku)

                    if (storeAccountTransaction != null) {
                        // Another customer is already entitled to this product on the same store account
                        throw MobilyPurchaseException(MobilyPurchaseException.Type.STORE_ACCOUNT_ALREADY_HAVE_PURCHASE)
                    }
                }
            }

            return builder.build()
        }
    }
}