package com.mobilyflow.mobilypurchasesdk.SDKHelpers

import android.os.Looper
import android.os.NetworkOnMainThreadException
import android.util.Log
import com.android.billingclient.api.Purchase
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientException
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MobilyPurchaseAPI
import com.mobilyflow.mobilypurchasesdk.Models.MobilyCustomerEntitlement
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import org.json.JSONArray

class MobilyPurchaseSDKSyncer(
    val API: MobilyPurchaseAPI,
    val billingClient: BillingClientWrapper,
) {
    private var customerId: String? = null

    var entitlements: List<MobilyCustomerEntitlement>? = null
    var storeAccountTransactions: List<BillingClientWrapper.PurchaseWithType>? = null

    private var lastSyncTime: Long? = null
    private val CACHE_DURATION_MS: Long = 3600 * 1000

    fun login(customerId: String?, jsonEntitlements: JSONArray?) {
        synchronized(this) {
            this.customerId = customerId
            this.entitlements = null
            this.lastSyncTime = null

            if (customerId != null && jsonEntitlements != null) {
                _syncEntitlements(jsonEntitlements)
                lastSyncTime = System.currentTimeMillis()
            }
        }
    }

    @Throws(MobilyException::class)
    fun ensureSync(force: Boolean = false) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw NetworkOnMainThreadException()
        }

        // Synchronise ensure a new call to ensureSync with wait the old one to finish
        synchronized(this) {
            if (
                force ||
                lastSyncTime == null ||
                (lastSyncTime!! + CACHE_DURATION_MS) < System.currentTimeMillis()
            ) {
                Logger.d("Run sync")
                _syncEntitlements()
                lastSyncTime = System.currentTimeMillis()
                Log.d("MobilyFlow", "End Sync")
            }
        }
    }

    @Throws(MobilyException::class)
    private fun _syncEntitlements(overrideJsonEntitlements: JSONArray? = null) {
        if (customerId == null) {
            return
        }

        try {
            this.storeAccountTransactions = this.billingClient.queryPurchases()
        } catch (e: BillingClientException) {
            Logger.e("[Syncer] BillingClientException: ${e.code} (${e.message})")
        }

        val entitlementsJson = overrideJsonEntitlements ?: this.API.getCustomerEntitlements(customerId!!)
        val entitlements = mutableListOf<MobilyCustomerEntitlement>()

        for (i in 0..<entitlementsJson.length()) {
            val jsonEntitlement = entitlementsJson.getJSONObject(i)
            entitlements.add(MobilyCustomerEntitlement.parse(jsonEntitlement, this.storeAccountTransactions))
        }

        this.entitlements = entitlements
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

    fun getStoreAccountTransaction(androidSku: String): Purchase? {
        return this.storeAccountTransactions?.find { tx ->
            tx.purchase.products.contains(androidSku)
        }?.purchase
    }
}