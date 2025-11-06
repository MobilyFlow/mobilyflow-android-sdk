package com.mobilyflow.mobilypurchasesdk.SDKHelpers

import android.os.Looper
import android.os.NetworkOnMainThreadException
import com.android.billingclient.api.Purchase
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientException
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MobilyPurchaseAPI
import com.mobilyflow.mobilypurchasesdk.Models.Entitlement.MobilyCustomerEntitlement
import com.mobilyflow.mobilypurchasesdk.Models.MobilyCustomer
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import org.json.JSONArray

class MobilyPurchaseSDKSyncer(
    val API: MobilyPurchaseAPI,
    val billingClient: BillingClientWrapper,
) {
    private var customer: MobilyCustomer? = null

    var entitlements: List<MobilyCustomerEntitlement>? = null
    var storeAccountTransactions: List<BillingClientWrapper.PurchaseWithType>? = null

    private var lastSyncTime: Long? = null
    private val CACHE_DURATION_MS: Long = 3600 * 1000

    fun login(customer: MobilyCustomer?, jsonEntitlements: JSONArray?) {
        synchronized(this) {
            this.customer = customer
            this.entitlements = null
            this.lastSyncTime = null

            if (customer != null && jsonEntitlements != null) {
                this._syncEntitlements(jsonEntitlements)
            }

            this.lastSyncTime = System.currentTimeMillis()
        }
    }

    fun logout() {
        synchronized(this) {
            this.customer = null
            this.entitlements = null
            this.lastSyncTime = null
        }
    }

    @Throws(MobilyException::class)
    fun ensureSync(force: Boolean = false) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw NetworkOnMainThreadException()
        }

        // Synchronise ensure a new call to ensureSync with wait the old one to finish
        synchronized(this) {
            if (customer != null && customer!!.forwardNotificationEnable) {
                // If a customer is flag as forwarded, we double check if it's still the case (so if we disable forwarding
                // on the backoffice, it's take effect instantly)
                val isForwardingEnable = this.API.isForwardingEnable(customer!!.externalRef)
                customer!!.forwardNotificationEnable = isForwardingEnable
            }

            if (
                force ||
                lastSyncTime == null ||
                (lastSyncTime!! + CACHE_DURATION_MS) < System.currentTimeMillis()
            ) {
                Logger.d("Run Sync expected...")
                if (customer != null) {
                    Logger.d("Run Sync for customer ${customer!!.id} (externalRef: ${customer!!.externalRef})")
                    _syncEntitlements()
                    lastSyncTime = System.currentTimeMillis()
                } else {
                    Logger.d(" -> Sync skipped (no customer)")
                }
                Logger.d("End Sync")
            }
        }
    }

    @Throws(MobilyException::class)
    private fun _syncEntitlements(overrideJsonEntitlements: JSONArray? = null) {
        try {
            this.storeAccountTransactions = this.billingClient.queryPurchases()
        } catch (e: BillingClientException) {
            Logger.e("[Syncer] BillingClientException: ${e.code} (${e.message})")
        }

        val entitlementsJson = overrideJsonEntitlements ?: this.API.getCustomerEntitlements(customer!!.id)
        val entitlements = mutableListOf<MobilyCustomerEntitlement>()

        for (i in 0..<entitlementsJson.length()) {
            val jsonEntitlement = entitlementsJson.getJSONObject(i)
            entitlements.add(
                MobilyCustomerEntitlement.parse(
                    jsonEntitlement,
                    this.storeAccountTransactions,
                )
            )
        }

        this.entitlements = entitlements
    }

    @Throws(MobilyException::class)
    fun getEntitlementForSubscription(subscriptionGroupId: String): MobilyCustomerEntitlement? {
        if (customer == null) {
            throw MobilyException(MobilyException.Type.NO_CUSTOMER_LOGGED)
        }

        for (entitlement in this.entitlements!!) {
            if (entitlement.type == ProductType.SUBSCRIPTION && entitlement.Product.subscription!!.groupId == subscriptionGroupId) {
                return entitlement
            }
        }
        return null
    }

    fun getEntitlement(productId: String): MobilyCustomerEntitlement? {
        if (customer == null) {
            throw MobilyException(MobilyException.Type.NO_CUSTOMER_LOGGED)
        }

        for (entitlement in this.entitlements!!) {
            if (entitlement.Product.id == productId) {
                return entitlement
            }
        }
        return null
    }

    fun getEntitlements(productIds: Array<String>?): List<MobilyCustomerEntitlement> {
        if (customer == null) {
            throw MobilyException(MobilyException.Type.NO_CUSTOMER_LOGGED)
        }

        val result = mutableListOf<MobilyCustomerEntitlement>()

        for (entitlement in this.entitlements!!) {
            if (productIds == null || productIds.contains(entitlement.Product.id)) {
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