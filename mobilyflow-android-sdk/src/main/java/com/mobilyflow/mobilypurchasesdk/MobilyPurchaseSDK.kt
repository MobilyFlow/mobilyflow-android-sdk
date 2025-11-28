package com.mobilyflow.mobilypurchasesdk

import android.app.Activity
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyTransferOwnershipStatus
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyPurchaseException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyTransferOwnershipException
import com.mobilyflow.mobilypurchasesdk.Models.Entitlement.MobilyCustomerEntitlement
import com.mobilyflow.mobilypurchasesdk.Models.Internal.PurchaseOptions
import com.mobilyflow.mobilypurchasesdk.Models.MobilyCustomer
import com.mobilyflow.mobilypurchasesdk.Models.MobilyEvent
import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Models.Product.MobilySubscriptionGroup

object MobilyPurchaseSDK {
    private var instance: MobilyPurchaseSDKImpl? = null

    /**
     *
     * Note for doc: You can call init multiple time with different config but it will cause currentUser to be logged out.
     */
    @JvmStatic
    @Throws(MobilyException::class)
    fun initialize(
        activity: Activity,
        appId: String,
        apiKey: String,
        environment: MobilyEnvironment,
        options: MobilyPurchaseSDKOptions? = null,
    ) {
        if (instance != null) {
            instance!!.reinit(appId, apiKey, environment, options)
        } else {
            instance = MobilyPurchaseSDKImpl(activity, appId, apiKey, environment, options)
        }
    }

    private fun ensureInit() {
        if (instance == null) {
            throw MobilyException(MobilyException.Type.SDK_NOT_INITIALIZED)
        }
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun close() {
        ensureInit()
        instance!!.close()
        instance = null
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun login(externalRef: String, activity: Activity? = null): MobilyCustomer {
        ensureInit()
        return instance!!.login(externalRef, activity)
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun logout() {
        ensureInit()
        instance!!.logout()
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun getProducts(identifiers: Array<String>?, onlyAvailable: Boolean): List<MobilyProduct> {
        ensureInit()
        return instance!!.getProducts(identifiers, onlyAvailable)
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun getSubscriptionGroups(identifiers: Array<String>?, onlyAvailable: Boolean): List<MobilySubscriptionGroup> {
        ensureInit()
        return instance!!.getSubscriptionGroups(identifiers, onlyAvailable)
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun getSubscriptionGroupById(id: String): MobilySubscriptionGroup {
        ensureInit()
        return instance!!.getSubscriptionGroupById(id)
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun DANGEROUS_getProductFromCacheWithId(id: String): MobilyProduct? {
        ensureInit()
        return instance!!.getProductFromCacheWithId(id)
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun getEntitlementForSubscription(subscriptionGroupId: String): MobilyCustomerEntitlement? {
        ensureInit()
        return instance!!.getEntitlementForSubscription(subscriptionGroupId)
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun getEntitlement(productId: String): MobilyCustomerEntitlement? {
        ensureInit()
        return instance!!.getEntitlement(productId)
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun getEntitlements(productIds: Array<String>?): List<MobilyCustomerEntitlement> {
        ensureInit()
        return instance!!.getEntitlements(productIds)
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun getExternalEntitlements(): List<MobilyCustomerEntitlement> {
        ensureInit()
        return instance!!.getExternalEntitlements()
    }

    @JvmStatic
    @Throws(MobilyException::class, MobilyTransferOwnershipException::class)
    fun requestTransferOwnership(): MobilyTransferOwnershipStatus {
        ensureInit()
        return instance!!.requestTransferOwnership()
    }

    @JvmStatic
    fun openManageSubscription() {
        ensureInit()
        return instance!!.openManageSubscription()
    }

    @JvmStatic
    @Throws(MobilyPurchaseException::class, MobilyException::class)
    fun purchaseProduct(
        activity: Activity,
        product: MobilyProduct,
        options: PurchaseOptions? = null,
    ): MobilyEvent {
        ensureInit()
        return instance!!.purchaseProduct(activity, product, options)
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun sendDiagnostic() {
        ensureInit()
        instance!!.sendDiagnostic()
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun getStoreCountry(): String? {
        ensureInit()
        return instance!!.getStoreCountry()
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun isBillingAvailable(): Boolean {
        ensureInit()
        return instance!!.isBillingAvailable()
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun isForwardingEnable(externalRef: String): Boolean {
        ensureInit()
        return instance!!.isForwardingEnable(externalRef)
    }

    @JvmStatic
    @Throws(MobilyException::class)
    fun getCustomer(): MobilyCustomer? {
        ensureInit()
        return instance!!.getCustomer()
    }

    @JvmStatic
    fun getSDKVersion(): String {
        return MOBILYFLOW_SDK_VERSION
    }
}