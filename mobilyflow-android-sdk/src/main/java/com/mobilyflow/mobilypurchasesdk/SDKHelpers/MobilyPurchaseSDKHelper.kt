package com.mobilyflow.mobilypurchasesdk.SDKHelpers

import com.android.billingclient.api.BillingFlowParams
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
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
                        val storeAccountTransaction = syncer.getStoreAccountTransaction(product)

                        if (storeAccountTransaction != null) {
                            // Another customer is already entitled to this product on the same store account
                            throw MobilyPurchaseException(MobilyPurchaseException.Type.STORE_ACCOUNT_ALREADY_HAVE_PURCHASE)
                        }
                    }
                }
            } else {
                val entitlement = if (product.subscriptionProduct!!.subscriptionGroupId != null) {
                    syncer.getEntitlementForSubscription(product.subscriptionProduct.subscriptionGroupId!!)
                } else {
                    syncer.getEntitlement(product.id)
                }

                if (entitlement != null) {
                    if (!entitlement.subscription!!.isManagedByThisStoreAccount) {
                        throw MobilyPurchaseException(MobilyPurchaseException.Type.NOT_MANAGED_BY_THIS_STORE_ACCOUNT)
                    }
                    
                    val currentSku = entitlement.product.android_sku
                    val currentBasePlan =
                        entitlement.product.subscriptionProduct!!.android_basePlanId

                    if (currentSku == product.android_sku && currentBasePlan == product.subscriptionProduct.android_basePlanId) {
                        /**
                         * TODO: Allow user to re-enable subscription is case activePurchase.isAutoRenewing
                         *
                         * Actually restarting the billingFlow for the same subscription cause the old one to be
                         * refund and a new one is restarted, so we disable this feature.
                         * The best way is to redirect user to "Manage Subscription" in the play store.
                         */
                        throw MobilyPurchaseException(MobilyPurchaseException.Type.ALREADY_PURCHASED)
                    }

                    // TODO: Manage UPGRADE & DOWNGRADE
                    // We should use DEFERRED for downgrade but it cause error "Requested replacement mode is not supported for this request"
                    builder.setSubscriptionUpdateParams(
                        BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                            .setOldPurchaseToken(entitlement.subscription.purchaseToken!!)
                            .setSubscriptionReplacementMode(
                                BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
                            )
                            .build()
                    )
                } else {
                    val storeAccountTransaction = if (product.subscriptionProduct.subscriptionGroupId != null) {
                        syncer.getStoreAccountTransactionForSubscription(product.subscriptionProduct.subscriptionGroupId)
                    } else {
                        syncer.getStoreAccountTransaction(product)
                    }

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