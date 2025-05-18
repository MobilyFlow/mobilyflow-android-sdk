package com.mobilyflow.mobilypurchasesdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientException
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientStatus
import com.mobilyflow.mobilypurchasesdk.BillingClientWrapper.BillingClientWrapper
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.ProductStatus
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Enums.TransferOwnershipStatus
import com.mobilyflow.mobilypurchasesdk.Enums.WebhookStatus
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyPurchaseException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyTransferOwnershipException
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MapTransactionItem
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MinimalProductForAndroidPurchase
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseAPI.MobilyPurchaseAPI
import com.mobilyflow.mobilypurchasesdk.Models.MobilyCustomer
import com.mobilyflow.mobilypurchasesdk.Models.MobilyCustomerEntitlement
import com.mobilyflow.mobilypurchasesdk.Models.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Models.MobilySubscriptionGroup
import com.mobilyflow.mobilypurchasesdk.Models.PurchaseOptions
import com.mobilyflow.mobilypurchasesdk.Monitoring.AppLifecycleProvider
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.mobilypurchasesdk.Monitoring.Monitoring
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseRegistry
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseSDKDiagnostics
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseSDKHelper
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseSDKSyncer
import com.mobilyflow.mobilypurchasesdk.SDKHelpers.MobilyPurchaseSDKWaiter
import com.mobilyflow.mobilypurchasesdk.Utils.Utils.Companion.getPreferredLocales
import org.json.JSONArray
import java.util.concurrent.Executors


class MobilyPurchaseSDK(
    val context: Context,
    val appId: String,
    val apiKey: String,
    val environment: MobilyEnvironment,
    options: MobilyPurchaseSDKOptions? = null,
) {
    private val API =
        MobilyPurchaseAPI(appId, apiKey, environment, getPreferredLocales(options?.locales), options?.apiURL)

    // TODO: Douple check this code doesn't duplicate if there is multiple SDK instances
    private val billingClient = BillingClientWrapper(context) { billingResult, purchases ->
        // Note: for out-of-app purchase, this function is called only when app is in background (but not when restart)
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            Executors.newSingleThreadExecutor().execute {
                try {
                    for (purchase in purchases) {
                        finishPurchase(purchase, true)
                    }
                } catch (e: Exception) {
                    Logger.e("billingClient out-of-app error", e)
                }
            }
        }
    }

    private var customer: MobilyCustomer? = null

    private val diagnostics: MobilyPurchaseSDKDiagnostics = MobilyPurchaseSDKDiagnostics(billingClient, null)
    private val waiter: MobilyPurchaseSDKWaiter = MobilyPurchaseSDKWaiter(API, diagnostics)
    private val syncer: MobilyPurchaseSDKSyncer = MobilyPurchaseSDKSyncer(API, billingClient)

    private var isPurchasing = false

    private var lastAppPauseTime: Long? = null
    private var lifecycleListener: AppLifecycleProvider.AppLifecycleCallbacks

    private val productsCaches = mutableMapOf<String, MobilyProduct>()

    init {
        Monitoring.initialize(context, "MobilyFlow", options?.debug == true) { logFile ->
            API.uploadMonitoring(customer?.id, logFile)
        }

        lifecycleListener = object : AppLifecycleProvider.AppLifecycleCallbacks() {
            override fun onActivityPaused(activity: Activity) {
                Logger.d("onActivityPaused")
                lastAppPauseTime = System.currentTimeMillis()
            }

            override fun onActivityResumed(activity: Activity) {
                Executors.newSingleThreadExecutor().execute {
                    // When activity resume, force sync after 2 minutes
                    Logger.d("onActivityResumed")
                    if (lastAppPauseTime != null && (lastAppPauseTime!! + 120 * 1000) < System.currentTimeMillis()) {
                        Logger.d("onActivityResumed -> FORCE SYNC")
                        runCatching {
                            syncer.ensureSync(true)
                        }
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
        this.customer = null
        diagnostics.customerId = null
        this.syncer.login(null, null)
        Monitoring.close()
        this.billingClient.endConnection()
    }

    /* ******************************************************************* */
    /* ****************************** LOGIN ****************************** */
    /* ******************************************************************* */

    fun login(externalRef: String): MobilyCustomer {
        // 1. Login
        val loginResponse = this.API.login(externalRef)

        this.customer = MobilyCustomer.parse(loginResponse.customer, loginResponse.isForwardingEnable)
        diagnostics.customerId = this.customer?.id
        this.syncer.login(customer, loginResponse.entitlements)

        // 2. Sync
        this.syncer.ensureSync()

        // 3. Manage out-of-app purchase
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val purchases = this.billingClient.queryPurchases()

                for (it in purchases) {
                    if (!it.purchase.isAcknowledged) {
                        finishPurchase(it.purchase, false, null)
                    }
                }

                // 4. Map transaction that are not known by the server
                val transactionsToMap = MobilyPurchaseSDKHelper.getTransactionsToMap(
                    loginResponse.platformOriginalTransactionIds,
                    purchases
                )

                if (transactionsToMap.isNotEmpty()) {
                    try {
                        this.API.mapTransactions(this.customer!!.id, transactionsToMap)
                    } catch (e: Exception) {
                        Logger.e("Map transactions error", e)
                    }
                }
            } catch (e: BillingClientException) {
                Logger.e("[Login] BillingClientException: ${e.code} (${e.message})")
            }
        }

        return customer!!
    }


    /* ******************************************************************* */
    /* **************************** PRODUCTS ***************************** */
    /* ******************************************************************* */

    @Throws(MobilyException::class)
    fun getProducts(identifiers: Array<String>?, onlyAvailable: Boolean): List<MobilyProduct> {
        try {
            // 1. Get product from Mobily API
            val jsonProducts = this.API.getProducts(identifiers)

            // 2. Get product from Play Store
            MobilyPurchaseRegistry.registerAndroidJsonProducts(jsonProducts, this.billingClient)

            // 3. Parse to MobilyProduct
            val mobilyProducts = arrayListOf<MobilyProduct>()

            for (i in 0..<jsonProducts.length()) {
                val jsonProduct = jsonProducts.getJSONObject(i)

                val mobilyProduct = MobilyProduct.parse(jsonProduct)
                productsCaches[mobilyProduct.id] = mobilyProduct

                if (!onlyAvailable || mobilyProduct.status == ProductStatus.AVAILABLE) {
                    mobilyProducts.add(mobilyProduct)
                }
            }

            return mobilyProducts
        } catch (e: MobilyException) {
            throw e
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR, e)
        }
    }

    @Throws(MobilyException::class)
    fun getSubscriptionGroups(
        identifiers: Array<String>?,
        onlyAvailable: Boolean
    ): List<MobilySubscriptionGroup> {
        try {
            // 1. Get product from Mobily API
            val jsonGroups = this.API.getSubscriptionGroups(identifiers)

            // 2. Get product from Play Store
            val allJsonProducts = JSONArray()
            for (i in 0..<jsonGroups.length()) {
                val jsonProducts = jsonGroups.getJSONObject(i).getJSONArray("Products")
                for (j in 0..<jsonProducts.length()) {
                    allJsonProducts.put(jsonProducts.getJSONObject(j))
                }
            }
            MobilyPurchaseRegistry.registerAndroidJsonProducts(allJsonProducts, this.billingClient)

            // 3. Parse to MobilySubscriptionGroup
            val mobilyGroups = arrayListOf<MobilySubscriptionGroup>()

            for (i in 0..<jsonGroups.length()) {
                val jsonGroup = jsonGroups.getJSONObject(i)

                val mobilyGroup = MobilySubscriptionGroup.parse(jsonGroup, onlyAvailable)

                for (product in mobilyGroup.products) {
                    productsCaches[product.id] = product
                }

                if (!onlyAvailable || mobilyGroup.products.isNotEmpty()) {
                    mobilyGroups.add(mobilyGroup)
                }
            }

            return mobilyGroups
        } catch (e: MobilyException) {
            throw e
        } catch (e: Exception) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR, e)
        }
    }

    fun getProductFromCacheWithId(id: String): MobilyProduct? {
        return productsCaches[id]
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
        if (customer == null) {
            throw MobilyException(MobilyException.Type.NO_CUSTOMER_LOGGED)
        }

        try {
            val purchases = this.billingClient.queryPurchases()
            val transactionsToClaim = MobilyPurchaseSDKHelper.getAllPurchaseTokens(purchases)

            if (transactionsToClaim.isNotEmpty()) {
                val requestId = this.API.transferOwnershipRequest(customer!!.id, transactionsToClaim)
                val status = this.waiter.waitTransferOwnershipWebhook(requestId)
                Logger.d("Request ownership transfer complete with status ${status.toString().lowercase()}")
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
        if (this.customer == null) {
            throw MobilyException(MobilyException.Type.NO_CUSTOMER_LOGGED)
        }

        synchronized(this) {
            if (this.isPurchasing) {
                throw MobilyPurchaseException(MobilyPurchaseException.Type.PURCHASE_ALREADY_PENDING)
            }
            this.isPurchasing = true
        }

        this.syncer.ensureSync()

        try {
            if (this.customer!!.isForwardingEnable) {
                throw MobilyPurchaseException(MobilyPurchaseException.Type.CUSTOMER_FORWARDED)
            }

            if (billingClient.getStatus() == BillingClientStatus.UNAVAILABLE) {
                throw MobilyException(MobilyException.Type.STORE_UNAVAILABLE)
            }

            Logger.d("Start purchaseProduct ${product.identifier}")

            val billingFlowParams =
                MobilyPurchaseSDKHelper.createBillingFlowParams(syncer, customer!!.id, product, options)

            val purchases = billingClient.launchBillingFlow(activity, billingFlowParams)

            // Process purchase
            if (purchases.isEmpty() || purchases.size != 1) {
                Logger.e("launchBillingFlow should only return a single purchase (actually ${purchases.size})")
                this.sendDiagnostic()

                throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
            }

            return finishPurchase(purchases[0], false, product)
        } catch (e: BillingClientException) {
            if (e.code == BillingClient.BillingResponseCode.USER_CANCELED) {
                throw MobilyPurchaseException(MobilyPurchaseException.Type.USER_CANCELED)
            } else if (e.code == BillingClient.BillingResponseCode.NETWORK_ERROR) {
                throw MobilyPurchaseException(MobilyPurchaseException.Type.NETWORK_UNAVAILABLE)
            } else if (e.code == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                throw MobilyPurchaseException(MobilyPurchaseException.Type.BILLING_ISSUE)
            } else if (
                e.code == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ||
                e.code == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
            ) {
                throw MobilyException(MobilyException.Type.STORE_UNAVAILABLE)
            } else {
                Logger.e("purchaseProduct unknown error", e)
                this.sendDiagnostic()

                throw MobilyPurchaseException(MobilyPurchaseException.Type.FAILED)
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

    private fun finishPurchase(
        purchase: Purchase,
        mapTransaction: Boolean,
        product: MobilyProduct? = null
    ): WebhookStatus {
        Logger.d("finishPurchase: ${purchase.orderId}")
        var status = WebhookStatus.ERROR

        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) {
            return status
        }

        // https://developer.android.com/google/play/billing/integrate#process
        if (purchase.products.size != 1) {
            Logger.d("finishPurchase: should only have one product (actually: ${purchase.products.size})")
            this.sendDiagnostic()
            return status
        }


        val androidSku = purchase.products[0]

        val minimalProduct: MinimalProductForAndroidPurchase
        if (product != null && product.android_sku == androidSku) {
            minimalProduct = MinimalProductForAndroidPurchase(
                type = product.type,
                isConsumable = product.oneTimeProduct?.isConsumable ?: false,
            )
        } else {
            try {
                minimalProduct = API.getMinimalProductForAndroidPurchase(androidSku)
            } catch (e: Exception) {
                Logger.e("Can't get minimal product for sku $androidSku, we can't finish transaction")
                return status
            }
        }

        if (minimalProduct.type == ProductType.ONE_TIME && minimalProduct.isConsumable) {
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

        if (mapTransaction && this.customer != null) {
            try {
                this.API.mapTransactions(
                    this.customer!!.id,
                    arrayOf(MapTransactionItem(androidSku, purchase.purchaseToken, minimalProduct.type))
                )
            } catch (e: Exception) {
                Logger.e("Map transaction error", e)
            }
        }

        if (this.customer != null) {
            runCatching {
                if (!this.customer!!.isForwardingEnable) {
                    status = this.waiter.waitPurchaseWebhook(purchase)
                }
                syncer.ensureSync(true)
            }
        }
        return status
    }

    /* *********************************************************** */
    /* *********************** DIAGNOSTICS *********************** */
    /* *********************************************************** */

    fun sendDiagnostic() {
        diagnostics.sendDiagnostic()
    }

    /* *********************************************************** */
    /* ************************* OTHERS ************************** */
    /* *********************************************************** */

    fun isForwardingEnable(externalRef: String): Boolean {
        return this.API.isForwardingEnable(externalRef)
    }

    fun getCustomer(): MobilyCustomer? {
        return this.customer
    }
}