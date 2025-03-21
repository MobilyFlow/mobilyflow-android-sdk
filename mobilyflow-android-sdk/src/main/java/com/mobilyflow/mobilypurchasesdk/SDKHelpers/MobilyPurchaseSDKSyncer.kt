package com.mobilyflow.mobilypurchasesdk.SDKHelpers

import android.os.Looper
import android.os.NetworkOnMainThreadException
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientException
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MobilyPurchaseAPI
import com.mobilyflow.mobilypurchasesdk.Models.MobilyCustomerEntitlement
import com.mobilyflow.mobilypurchasesdk.Models.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Models.MobilySubscriptionGroup
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger

class MobilyPurchaseSDKSyncer(
    val API: MobilyPurchaseAPI,
    val billingClient: BillingClientWrapper,
) {
    private var customerId: String? = null

    var products: List<MobilyProduct>? = null
    var subscriptionGroups: List<MobilySubscriptionGroup>? = null
    var entitlements: List<MobilyCustomerEntitlement>? = null
    var storeAccountTransactions: List<BillingClientWrapper.PurchaseWithType>? = null

    private var lastProductFetchTime: Long? = null
    private val CACHE_DURATION_MS: Long = 3600 * 1000

    @Throws(MobilyException::class)
    fun ensureSync(force: Boolean = false) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw NetworkOnMainThreadException()
        }

        // Synchronise ensure a new call to ensureSync with wait the old one to finish
        synchronized(this) {
            if (
                force ||
                lastProductFetchTime == null ||
                (lastProductFetchTime!! + CACHE_DURATION_MS) < System.currentTimeMillis()
            ) {
                lastProductFetchTime = System.currentTimeMillis()
                Logger.d("Run sync")
                _syncProducts()
                _syncEntitlements()
                Log.d("MobilyFlow", "End Sync")
            }
        }
    }

    fun login(customerId: String?) {
        synchronized(this) {
            this.customerId = customerId
            this.entitlements = null
            this.lastProductFetchTime = null
        }
    }

    @Throws(MobilyException::class)
    private fun _syncProducts() {
        try {
            // 1. Get product from Mobily API
            val jsonProducts = this.API.getProducts(null)

            // 2. Get product from Play Store
            val subsIds = arrayListOf<String>()
            val iapIds = arrayListOf<String>()
            for (i in 0..<jsonProducts.length()) {
                val sku = jsonProducts.getJSONObject(i).getString("android_sku")
                val type = jsonProducts.getJSONObject(i).getString("type")

                if (type == "one_time") {
                    if (!iapIds.contains(sku)) {
                        iapIds.add(sku)
                    }
                } else {
                    if (!subsIds.contains(sku)) {
                        subsIds.add(sku)
                    }
                }
            }

            val storeProducts = this.billingClient.getProducts(subsIds, iapIds)
            MobilyPurchaseRegistry.registerAndroidProducts(storeProducts)

            // 3. Parse to MobilyProduct
            val mobilyProducts = arrayListOf<MobilyProduct>()
            val subscriptionGroupMap = mutableMapOf<String, MobilySubscriptionGroup>()

            for (i in 0..<jsonProducts.length()) {
                val jsonProduct = jsonProducts.getJSONObject(i)

                val mobilyProduct = MobilyProduct.parse(jsonProduct)
                mobilyProducts.add(mobilyProduct)

                if (mobilyProduct.subscriptionProduct?.subscriptionGroup != null) {
                    subscriptionGroupMap[mobilyProduct.subscriptionProduct.subscriptionGroupId!!] =
                        mobilyProduct.subscriptionProduct.subscriptionGroup
                }
            }

            products = mobilyProducts
            subscriptionGroups = subscriptionGroupMap.values.toList()
        } catch (e: MobilyException) {
            throw e
        } catch (e: BillingClientException) {
            when (e.code) {
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.NETWORK_ERROR ->
                    throw MobilyException(MobilyException.Type.STORE_UNAVAILABLE)

                else -> throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
            }
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR, e)
        }
    }

    @Throws(MobilyException::class)
    private fun _syncEntitlements() {
        if (customerId == null) {
            return
        }

        try {
            this.storeAccountTransactions = this.billingClient.queryPurchases()
        } catch (e: BillingClientException) {
            Logger.e("[Syncer] BillingClientException: ${e.code} (${e.message})")
            throw MobilyException(MobilyException.Type.STORE_UNAVAILABLE)
        }

        val entitlementsJson = this.API.getCustomerEntitlements(customerId!!)
        val entitlements = mutableListOf<MobilyCustomerEntitlement>()

        for (i in 0..<entitlementsJson.length()) {
            val jsonEntitlement = entitlementsJson.getJSONObject(i)
            entitlements.add(MobilyCustomerEntitlement.parse(jsonEntitlement, this.storeAccountTransactions))
        }

        this.entitlements = entitlements
    }

    @Throws(MobilyException::class)
    fun syncProducts() {
        synchronized(this) {
            _syncProducts()
        }
    }

    @Throws(MobilyException::class)
    fun syncEntitlements() {
        synchronized(this) {
            _syncEntitlements()
        }
    }

    @Throws(MobilyException::class)
    fun getProducts(identifiers: Array<String>?, onlyAvailable: Boolean): List<MobilyProduct> {
        this.ensureSync()

        val result = mutableListOf<MobilyProduct>()

        for (p in this.products!!) {
            if (!onlyAvailable || p.status == ProductStatus.AVAILABLE) {
                if (identifiers == null || identifiers.contains(p.identifier)) {
                    result.add(p)
                }
            }
        }

        return result
    }

    @Throws(MobilyException::class)
    fun getSubscriptionGroups(
        identifiers: Array<String>?,
        onlyAvailable: Boolean
    ): List<MobilySubscriptionGroup> {
        this.ensureSync()

        val result = mutableListOf<MobilySubscriptionGroup>()

        for (group in this.subscriptionGroups!!) {
            val products = mutableListOf<MobilyProduct>()
            for (p in this.products!!) {
                if (!onlyAvailable || p.status == ProductStatus.AVAILABLE) {
                    if (group.id == p.subscriptionProduct?.subscriptionGroupId) {
                        products.add(p)
                    }
                }
            }

            if (identifiers == null || identifiers.contains(group.identifier)) {
                if (!onlyAvailable || products.isNotEmpty()) {
                    val addedGroup = group.clone()
                    addedGroup.products = products
                    result.add(addedGroup)
                }
            }
        }

        return result
    }

    @Throws(MobilyException::class)
    fun getEntitlementForSubscription(subscriptionGroupId: String): MobilyCustomerEntitlement? {
        if (customerId == null) {
            throw MobilyException(MobilyException.Type.NO_CUSTOMER_LOGGED)
        }

        for (entitlement in this.entitlements!!) {
            if (entitlement.type == ProductType.SUBSCRIPTION && entitlement.product.subscriptionProduct!!.subscriptionGroupId == subscriptionGroupId) {
                return entitlement
            }
        }
        return null
    }

    fun getEntitlement(productId: String): MobilyCustomerEntitlement? {
        if (customerId == null) {
            throw MobilyException(MobilyException.Type.NO_CUSTOMER_LOGGED)
        }

        for (entitlement in this.entitlements!!) {
            if (entitlement.product.id == productId) {
                return entitlement
            }
        }
        return null
    }

    fun getEntitlements(productIds: Array<String>): List<MobilyCustomerEntitlement> {
        if (customerId == null) {
            throw MobilyException(MobilyException.Type.NO_CUSTOMER_LOGGED)
        }

        val result = mutableListOf<MobilyCustomerEntitlement>()

        for (entitlement in this.entitlements!!) {
            if (productIds.contains(entitlement.product.id)) {
                result.add(entitlement)
            }
        }
        return result
    }

    fun getStoreAccountTransaction(product: MobilyProduct): Purchase? {
        return this.storeAccountTransactions?.find { tx ->
            tx.purchase.products.contains(product.android_sku)
        }?.purchase
    }

    fun getStoreAccountTransactionForSubscription(subscriptionGroupId: String): Purchase? {
        val productsInGroup = products?.filter { p ->
            p.subscriptionProduct?.subscriptionGroupId == subscriptionGroupId
        }
        if (productsInGroup.isNullOrEmpty()) {
            return null
        }

        return this.storeAccountTransactions?.find(fun(tx): Boolean {
            for (product in productsInGroup) {
                if (tx.purchase.products.contains(product.android_sku)) {
                    return true
                }
            }
            return false
        })?.purchase
    }
}