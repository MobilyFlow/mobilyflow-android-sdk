package com.mobilyflow.mobilypurchasesdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientException
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Enums.TransferOwnershipStatus
import com.mobilyflow.mobilypurchasesdk.Enums.WebhookStatus
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyPurchaseException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyTransferOwnershipException
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MapTransactionItem
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MobilyPurchaseAPI
import com.mobilyflow.mobilypurchasesdk.Models.MobilyCustomerEntitlement
import com.mobilyflow.mobilypurchasesdk.Models.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Models.MobilySubscriptionGroup
import com.mobilyflow.mobilypurchasesdk.Models.PurchaseOptions
import com.mobilyflow.mobilypurchasesdk.Monitoring.AppLifecycleProvider
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.Monitoring.Monitoring
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseSDKDiagnostics
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseSDKHelper
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseSDKSyncer
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseSDKWaiter
import com.mobilyflow.mobilypurchasesdk.Utils.Utils.Companion.getPreferredLanguages
import java.util.concurrent.Executors


class MobilyPurchaseSDK(
    val context: Context,
    val appId: String,
    val apiKey: String,
    val environment: MobilyEnvironment,
    options: MobilyPurchaseSDKOptions? = null,
) {
    private val API =
        MobilyPurchaseAPI(appId, apiKey, environment, getPreferredLanguages(options?.languages), options?.apiURL)
    private val billingClient = BillingClientWrapper(context) { billingResult, purchases ->
        // Note: for out-of-app purchase, this function is called only when app is in background (but not when restart)
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            Executors.newSingleThreadExecutor().execute {
                this.syncer.ensureSync()
                for (purchase in purchases) {
                    finishPurchase(purchase, true)
                }
                this.syncer.syncEntitlements()
            }
        }
    }

    private var customerId: String? = null

    private val diagnostics: MobilyPurchaseSDKDiagnostics = MobilyPurchaseSDKDiagnostics(billingClient, null)
    private val waiter: MobilyPurchaseSDKWaiter = MobilyPurchaseSDKWaiter(API, diagnostics)
    private val syncer: MobilyPurchaseSDKSyncer = MobilyPurchaseSDKSyncer(API, billingClient)

    private var isPurchasing = false

    private var lastAppPauseTime: Long? = null
    private var lifecycleListener: AppLifecycleProvider.AppLifecycleCallbacks

    init {
        Log.d("MobilyFlow", "Update binary")
        Monitoring.initialize(context, "MobilyFlow", options?.debug == true) { logFile ->
            API.uploadMonitoring(customerId, logFile)
        }

        lifecycleListener = object : AppLifecycleProvider.AppLifecycleCallbacks() {
            override fun onActivityPaused(activity: Activity) {
                lastAppPauseTime = System.currentTimeMillis()
            }

            override fun onActivityResumed(activity: Activity) {
                Executors.newSingleThreadExecutor().execute {
                    // When activity resume, force sync after 2 minutes
                    if (lastAppPauseTime != null && (lastAppPauseTime!! + 120 * 1000) < System.currentTimeMillis()) {
                        syncer.ensureSync(true)
                    }
                }
            }

            override fun uncaughtException(t: Thread?, e: Throwable?) {
                Logger.logger?.flush()
                sendDiagnostic()
            }
        }
        AppLifecycleProvider.registerListener(lifecycleListener)
    }

    fun close() {
        AppLifecycleProvider.unregisterListener(lifecycleListener)
        this.customerId = null
        diagnostics.customerId = null
        this.syncer.login(null)
        Monitoring.close()
        this.billingClient.endConnection()
    }

    /* ******************************************************************* */
    /* ****************************** LOGIN ****************************** */
    /* ******************************************************************* */

    fun login(externalId: String) {
        // 1. Login
        val loginResponse = this.API.login(externalId)

        this.customerId = loginResponse.customerId
        diagnostics.customerId = this.customerId
        this.syncer.login(customerId)

        try {
            // 2. Sync
            this.syncer.ensureSync()
            val purchases = this.billingClient.queryPurchases()

            // 3. Manage out-of-app purchase
            for (it in purchases) {
                if (!it.purchase.isAcknowledged) {
                    finishPurchase(it.purchase, false)
                }
            }

            // 4. Map transaction that are not known by the server
            val transactionsToMap = MobilyPurchaseSDKHelper.getTransactionsToMap(
                loginResponse.platformOriginalTransactionIds,
                purchases
            )

            if (transactionsToMap.isNotEmpty()) {
                try {
                    this.API.mapTransactions(this.customerId!!, transactionsToMap)
                } catch (e: Exception) {
                    Logger.e("Map transactions error", e)
                }
            }
        } catch (e: BillingClientException) {
            Logger.e("[Login] BillingClientException: ${e.code} (${e.message})")
            throw MobilyException(MobilyException.Type.STORE_UNAVAILABLE)
        }
    }


    /* ******************************************************************* */
    /* **************************** PRODUCTS ***************************** */
    /* ******************************************************************* */

    @Throws(MobilyException::class)
    fun getProducts(identifiers: Array<String>?, onlyAvailable: Boolean): List<MobilyProduct> {
        return this.syncer.getProducts(identifiers, onlyAvailable)
    }

    @Throws(MobilyException::class)
    fun getSubscriptionGroups(
        identifiers: Array<String>?,
        onlyAvailable: Boolean
    ): List<MobilySubscriptionGroup> {
        return this.syncer.getSubscriptionGroups(identifiers, onlyAvailable)
    }

    /* ******************************************************************* */
    /* ************************** ENTITLEMENTS *************************** */
    /* ******************************************************************* */

    @Throws(MobilyException::class)
    fun getEntitlementForSubscription(subscriptionGroupId: String): MobilyCustomerEntitlement? {
        return this.syncer.getEntitlementForSubscription(subscriptionGroupId)
    }

    @Throws(MobilyException::class)
    fun getEntitlement(productId: String): MobilyCustomerEntitlement? {
        return this.syncer.getEntitlement(productId)
    }

    @Throws(MobilyException::class)
    fun getEntitlements(productIds: Array<String>): List<MobilyCustomerEntitlement> {
        return this.syncer.getEntitlements(productIds)
    }

    @Throws(MobilyException::class, MobilyTransferOwnershipException::class)
    fun requestTransferOwnership(): TransferOwnershipStatus {
        if (customerId == null) {
            throw MobilyException(MobilyException.Type.NO_CUSTOMER_LOGGED)
        }

        try {
            val purchases = this.billingClient.queryPurchases()
            val transactionsToClaim = MobilyPurchaseSDKHelper.getAllPurchaseTokens(purchases)

            if (transactionsToClaim.isNotEmpty()) {
                val requestId = this.API.transferOwnershipRequest(customerId!!, transactionsToClaim)
                val status = this.waiter.waitTransferOwnershipWebhook(requestId)
                Logger.d("Request ownership transfer complete with status ${status.value}")
                return status
            } else {
                return TransferOwnershipStatus.ACKNOWLEDGED
            }
        } catch (e: BillingClientException) {
            Logger.e("[TransferOwnership] BillingClientException: ${e.code} (${e.message})")
            throw MobilyException(MobilyException.Type.STORE_UNAVAILABLE)
        }
    }

    /* ******************************************************************* */
    /* ************************ INTERFACE HELPERS ************************ */
    /* ******************************************************************* */

    fun openManageSubscription() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setData(Uri.parse("https://play.google.com/store/account/subscriptions"))
        intent.putExtra("package", this.context.packageName)
        this.context.startActivity(intent)
    }

    /* ******************************************************************* */
    /* **************************** PURCHASE ***************************** */
    /* ******************************************************************* */

    @Throws(MobilyPurchaseException::class, MobilyException::class)
    fun purchaseProduct(
        activity: Activity,
        product: MobilyProduct,
        options: PurchaseOptions? = null,
    ): WebhookStatus {
        if (this.customerId == null) {
            throw MobilyException(MobilyException.Type.NO_CUSTOMER_LOGGED)
        }

        synchronized(this) {
            if (this.isPurchasing) {
                throw MobilyPurchaseException(MobilyPurchaseException.Type.PURCHASE_ALREADY_PENDING)
            }
            this.isPurchasing = true
        }

        try {
            this.syncer.ensureSync()
            Logger.d("Start purchaseProduct ${product.identifier}")

            val purchases = billingClient.launchBillingFlow(
                activity,
                MobilyPurchaseSDKHelper.createBillingFlowParams(syncer!!, customerId!!, product, options)
            )

            // Process purchase
            if (purchases.isEmpty() || purchases.size != 1) {
                Logger.e("launchBillingFlow should only return a single purchase (actually ${purchases.size})")
                this.sendDiagnostic()

                throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
            }

            finishPurchase(purchases[0], false)
            val status = this.waiter.waitPurchaseWebhook(purchases[0].orderId!!)
            this.syncer.syncEntitlements()

            return status
        } catch (e: BillingClientException) {
            if (e.code == BillingClient.BillingResponseCode.USER_CANCELED) {
                throw MobilyPurchaseException(MobilyPurchaseException.Type.USER_CANCELED)
            } else if (e.code == BillingClient.BillingResponseCode.NETWORK_ERROR) {
                throw MobilyPurchaseException(MobilyPurchaseException.Type.NETWORK_UNAVAILABLE)
            } else if (
                e.code == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ||
                e.code == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ||
                e.code == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
            ) {
                throw MobilyException(MobilyException.Type.STORE_UNAVAILABLE)
            } else {
                Logger.e("purchaseProduct unknown error", e)
                this.sendDiagnostic()

                throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
            }
        } catch (e: MobilyException) {
            if (e.type == MobilyException.Type.UNKNOWN_ERROR) {
                Logger.e("purchaseProduct unknown error", e)
                this.sendDiagnostic()
            }

            throw e
        } finally {
            this.isPurchasing = false
        }
    }

    /* ******************************************************************* */
    /* ****************** UPDATE TRANSACTION LISTENERS ******************* */
    /* ******************************************************************* */

    private fun finishPurchase(purchase: Purchase, mapTransaction: Boolean) {
        Logger.d("finishPurchase: ${purchase.orderId}")


        // https://developer.android.com/google/play/billing/integrate#process
        if (purchase.products.size != 1) {
            Logger.d("finishPurchase: should only have one product (actually: ${purchase.products.size})")
            this.sendDiagnostic()
            return
        }

        // Note: this can return the wrong product for subscription but we are only searching the product type so it's fine
        val product = this.syncer.products!!.find { p -> p.android_sku == purchase.products[0] }
        if (product == null) {
            Logger.d("finishPurchase: Can't find product with SKU: ${purchase.products[0]}")
            this.sendDiagnostic()
            return
        }

        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) {
            return
        }

        if (product.type == ProductType.ONE_TIME && product.oneTimeProduct!!.isConsumable) {
            // Consumable
            val consumeParams =
                ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

            // Note: consume also do an implicit acknowledge
            this.billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
                Logger.d("finishPurchase: consumeAsync result: ${billingResult.responseCode}/${billingResult.debugMessage}, purchaseToken: $purchaseToken")
            }
        } else {
            // Non-consumable & Subscription
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            this.billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                Logger.d("finishPurchase: acknowledgePurchase result: ${billingResult.responseCode}/${billingResult.debugMessage}")
            }
        }

        if (mapTransaction) {
            try {
                this.API.mapTransactions(
                    this.customerId!!,
                    arrayOf(MapTransactionItem(product.android_sku, purchase.purchaseToken, product.type))
                )
            } catch (e: Exception) {
                Logger.e("Map transaction error", e)
            }
        }
    }

    /* *********************************************************** */
    /* *********************** DIAGNOSTICS *********************** */
    /* *********************************************************** */

    fun sendDiagnostic() {
        diagnostics.sendDiagnostic()
    }
}