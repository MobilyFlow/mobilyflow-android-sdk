package com.mobilyflow.mobilypurchasesdk.BillingClientWrapper

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.os.NetworkOnMainThreadException
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingConfig
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.GetBillingConfigParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyPurchaseException
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger

class BillingClientWrapper(
    internal val context: Context,
    val purchasesUpdatedListener: PurchasesUpdatedListener
) : Object(), PurchasesUpdatedListener, BillingClientStateListener, PurchasesResponseListener {
    private val _client: BillingClient
    private var status: BillingClientStatus
    private var clientConfig: BillingConfig? = null

    private var billingFlowResult: BillingRequestResult<List<Purchase>?>? = null
    private var queryPurchaseResult: BillingRequestResult<List<Purchase>?>? = null

    class PurchaseWithType(val purchase: Purchase, val type: ProductType) {}

    init {
        this.status = BillingClientStatus.INITIALIZING
        this._client = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        _client.startConnection(this)
    }

    @Throws(BillingClientException::class)
    private fun ensureInitialization() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw NetworkOnMainThreadException()
        }

        synchronized(this) {
            if (this.status == BillingClientStatus.INITIALIZING) {
                this.wait()
            } else if (this.status != BillingClientStatus.AVAILABLE) {
                // Retry connection
                _client.startConnection(this)
                this.syncConfig()
                this.wait()

                if (this.status == BillingClientStatus.UNAVAILABLE) {
                    throw BillingClientException(
                        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                        "Billing Unavailable"
                    )
                }
            }
        }
    }

    private fun syncConfig() {
        val response = BillingRequestResult<BillingConfig?>()

        _client.getBillingConfigAsync(GetBillingConfigParams.newBuilder().build(), { result, config ->
            response.set(result, config)
        })

        response.waitResult()
        this.clientConfig = response.dataOptional()
    }

    fun getConfig(): BillingConfig? {
        return this.clientConfig
    }

    fun endConnection() {
        this._client.endConnection()
    }

    private fun _getProducts(products: List<QueryProductDetailsParams.Product>): BillingRequestResult<List<ProductDetails>> {
        val result = BillingRequestResult<List<ProductDetails>>()

        val request = QueryProductDetailsParams.newBuilder().setProductList(products).build()

        this._client.queryProductDetailsAsync(request) { billingResult, productDetails ->
            result.set(billingResult, productDetails)
        }

        result.waitResult()
        return result
    }

    @Throws(BillingClientException::class)
    fun getProducts(subsIds: List<String>?, iapIds: List<String>?): List<ProductDetails> {
        this.ensureInitialization()

        val result = arrayListOf<ProductDetails>()

        // Request have to be done separately to get SUBS and IAP
        // Else we got the error: java.lang.IllegalArgumentException: All products should be of the same product type.

        if (!subsIds.isNullOrEmpty()) {
            val products = arrayListOf<QueryProductDetailsParams.Product>()
            for (pId in subsIds) {
                val builder = QueryProductDetailsParams.Product.newBuilder()
                builder.setProductId(pId)
                builder.setProductType(BillingClient.ProductType.SUBS)
                products.add(builder.build())
            }

            val request = this._getProducts(products)

            val res = request.billingResult()
            if (res.responseCode != BillingClient.BillingResponseCode.OK) {
                throw BillingClientException(res.responseCode, res.debugMessage)
            }

            result.addAll(request.data())
        }
        if (!iapIds.isNullOrEmpty()) {
            val products = arrayListOf<QueryProductDetailsParams.Product>()
            for (pId in iapIds) {
                val builder = QueryProductDetailsParams.Product.newBuilder()
                builder.setProductId(pId)
                builder.setProductType(BillingClient.ProductType.INAPP)
                products.add(builder.build())
            }

            val request = this._getProducts(products)

            val res = request.billingResult()
            if (res.responseCode != BillingClient.BillingResponseCode.OK) {
                throw BillingClientException(res.responseCode, res.debugMessage)
            }

            result.addAll(request.data())
        }

        return result
    }

    @Throws(BillingClientException::class, MobilyPurchaseException::class)
    fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams
    ): List<Purchase> {
        this.ensureInitialization()

        if (this.billingFlowResult != null) {
            throw MobilyPurchaseException(MobilyPurchaseException.Type.PURCHASE_ALREADY_PENDING)
        }

        this.billingFlowResult = BillingRequestResult()

        var billingResult = _client.launchBillingFlow(activity, params)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            this.billingFlowResult = null
            throw BillingClientException(billingResult.responseCode, billingResult.debugMessage)
        }

        this.billingFlowResult!!.waitResult()

        billingResult = this.billingFlowResult!!.billingResult()
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            this.billingFlowResult = null
            throw BillingClientException(billingResult.responseCode, billingResult.debugMessage)
        }

        var purchases = this.billingFlowResult!!.dataOptional()
        if (purchases == null) {
            purchases = arrayListOf()
        }

        this.billingFlowResult = null

        return purchases
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (this.billingFlowResult != null) {
            Logger.d("onPurchasesUpdated: $billingResult -> Managed by wrapper internally")
            this.billingFlowResult!!.set(billingResult, purchases)
        } else {
            Logger.d("onPurchasesUpdated: $billingResult -> Send to global listener")
            this.purchasesUpdatedListener.onPurchasesUpdated(billingResult, purchases)
        }
    }

    @Throws(BillingClientException::class)
    fun consumeAsync(params: ConsumeParams, callback: ConsumeResponseListener) {
        this.ensureInitialization()
        this._client.consumeAsync(params, callback)
    }

    @Throws(BillingClientException::class)
    fun acknowledgePurchase(
        params: AcknowledgePurchaseParams,
        callback: AcknowledgePurchaseResponseListener
    ) {
        this.ensureInitialization()
        this._client.acknowledgePurchase(params, callback)
    }

    @Throws(BillingClientException::class)
    private fun _queryPurchases(type: String): List<Purchase> {
        if (this.queryPurchaseResult != null) {
            this.queryPurchaseResult?.waitResult()
        }

        this.queryPurchaseResult = BillingRequestResult()

        this._client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(type).build(),
            this
        )
        this.queryPurchaseResult!!.waitResult()

        val result = this.queryPurchaseResult!!.billingResult()
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            throw BillingClientException(result.responseCode, result.debugMessage)
        }

        var purchases = this.queryPurchaseResult!!.dataOptional()
        if (purchases == null) {
            purchases = arrayListOf()
        }

        this.queryPurchaseResult = null

        return purchases
    }

    /**
     * Query all purchase owned by the current user,
     *
     * @param type Type of the product, null to query all products
     */
    @Throws(BillingClientException::class)
    fun queryPurchases(type: ProductType? = null): List<PurchaseWithType> {
        this.ensureInitialization()

        val result = arrayListOf<PurchaseWithType>()

        if (type == null || type == ProductType.SUBSCRIPTION) {
            val subs = this._queryPurchases(BillingClient.ProductType.SUBS)
            for (sub in subs) {
                result.add(PurchaseWithType(sub, ProductType.SUBSCRIPTION))
            }
        }

        if (type == null || type == ProductType.ONE_TIME) {
            val items = this._queryPurchases(BillingClient.ProductType.INAPP)
            for (item in items) {
                result.add(PurchaseWithType(item, ProductType.ONE_TIME))
            }
        }

        return result
    }

    override fun onQueryPurchasesResponse(
        billingResult: BillingResult,
        purchases: List<Purchase>
    ) {
        if (this.queryPurchaseResult != null) {
            this.queryPurchaseResult!!.set(billingResult, purchases)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        synchronized(this) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // The BillingClient is ready. You can query purchases here.
                this.status = BillingClientStatus.AVAILABLE
                this.notifyAll()
            } else {
                this.status = BillingClientStatus.UNAVAILABLE
                this.notifyAll()
                // Code: https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponseCode
                // Code 3: Google Play In-app Billing API version is less than 3 BILLING_UNAVAILABLE
                // BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
                //  - Play Store Service unavailable (or user not logged)
                //  - Unsupported country
                //  - Google Play is unable to charge the user’s payment method

                Logger.e("onBillingSetupFinished: Error: ${billingResult.responseCode}/${billingResult.debugMessage}")
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        // Set the connection as disconnected, never call startConnection again, it will be done
        // automatically, either by the OS or by calling ensureInitialization on the next function call
        Logger.e("onBillingServiceDisconnected")
        this.status = BillingClientStatus.DISCONNECTED
    }

    fun getStatus(): BillingClientStatus {
        return this.status
    }
}
