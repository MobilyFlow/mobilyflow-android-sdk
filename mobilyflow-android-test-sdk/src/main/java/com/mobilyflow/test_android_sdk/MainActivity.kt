package com.mobilyflow.test_android_sdk

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyPurchaseException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyTransferOwnershipException
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseSDK
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseSDKOptions
import com.mobilyflow.mobilypurchasesdk.Models.MobilyCustomer
import com.mobilyflow.mobilypurchasesdk.Models.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Models.MobilySubscriptionOffer
import com.mobilyflow.mobilypurchasesdk.Models.PurchaseOptions
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import com.mobilyflow.test_android_sdk.ui.theme.MobilyflowAndroidSDKTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private var mobily: MobilyPurchaseSDK? = null

    private var products = MutableLiveData<List<MobilyProduct>?>()
    private var error = MutableLiveData<String?>()

    private var customer: MobilyCustomer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mobily = MobilyPurchaseSDK(
            this,
            appId = "caecc000-45ce-49b3-b218-46c1d985ae85",
            apiKey = "7aa18f9720a5c9731d17f5c54e89bdd218647f71269eed2f6c27c8fa5924da84",
            environment = MobilyEnvironment.DEVELOPMENT,
            options = MobilyPurchaseSDKOptions(
                locales = null,
                debug = true,
//                apiURL = "https://mobilyflow.eu-1.sharedwithexpose.com/v1/"
                apiURL = "https://api-staging.mobilyflow.com/v1/"
            )
        )

        setContent {
            MobilyflowAndroidSDKTheme {
                val products: List<MobilyProduct>? by this.products.observeAsState(null)
                val error: String? by this.error.observeAsState(null)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(30.dp),
                        modifier = Modifier
                            .padding(
                                start = 10.dp,
                                end = 10.dp,
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding(),
                            )
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = "Hello Mobily")

                        if (products == null && error == null) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(64.dp),
                            )
                        } else {
                            if (error != null) {
                                Text(text = error!!, color = Color.Red)
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    products!!.forEach { p ->
                                        IAPButton(
                                            activity = this@MainActivity,
                                            sdk = mobily!!,
                                            product = p,
                                            offer = null,
                                        )

                                        if (p.type == ProductType.SUBSCRIPTION && p.subscriptionProduct!!.promotionalOffers.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier.padding(
                                                    horizontal = 20.dp,
                                                    vertical = 0.dp
                                                ),
                                                verticalArrangement = Arrangement.spacedBy(30.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                p.subscriptionProduct!!.promotionalOffers.forEach { offer ->
                                                    IAPButton(
                                                        activity = this@MainActivity,
                                                        sdk = mobily!!,
                                                        product = p,
                                                        offer = offer,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(onClick = {
                                    this@MainActivity.products.postValue(null)
                                    this@MainActivity.error.postValue(null)
                                    this@MainActivity.loadData()
                                }) {
                                    Text(text = "Reload Mobily")
                                }
                                Button(onClick = {
                                    this@MainActivity.mobily!!.openManageSubscription()
                                }) {
                                    Text(text = "Manage Subscription")
                                }
                                Button(onClick = {
                                    Executors.newSingleThreadExecutor().execute {
                                        try {
                                            this@MainActivity.mobily!!.requestTransferOwnership()
                                        } catch (error: MobilyException) {
                                            Log.e(
                                                "MobilyFlow",
                                                "RequestTransferOwnership MobilyFetchException ${error.type}",
                                                error
                                            )
                                        } catch (error: MobilyTransferOwnershipException) {
                                            Log.e(
                                                "MobilyFlow",
                                                "RequestTransferOwnership MobilyTransferOwnershipException ${error.type}",
                                                error
                                            )
                                        } catch (error: Exception) {
                                            Log.e(
                                                "MobilyFlow",
                                                "RequestTransferOwnership error",
                                                error
                                            )
                                        }
                                    }
                                }) {
                                    Text(text = "Transfer Ownership")
                                }
                                Button(onClick = {
                                    PurchaseProductHelper(this@MainActivity, customer!!.id, "unregistered_item")
                                }) {
                                    Text(text = "Buy unregistered product")
                                }
                                Button(onClick = {
                                    PurchaseProductHelper(
                                        this@MainActivity,
                                        customer!!.id,
                                        "unregistered_subscription",
                                        "unregistered-subscription-1week"
                                    )
                                }) {
                                    Text(text = "Buy unregistered subscription")
                                }
                                Button(onClick = {
                                    PurchaseProductHelper(
                                        this@MainActivity,
                                        customer!!.id,
                                        "premium_test_sub",
                                        "premium-1month"
                                    )
                                }) {
                                    Text(text = "Outside Buy premium-1-month")
                                }
                            }
                        }
                    }
                }
            }
        }

        loadData()
    }

    override fun onDestroy() {
        this.mobily?.close()
        super.onDestroy()
    }

    fun loadData() {
        // Source: https://www.simplifiedcoding.net/android-asynctask/
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            /*
            * Your task will be executed here
            * you can execute anything here that
            * you cannot execute in UI thread
            * for example a network operation
            * This is a background thread and you cannot
            * access view elements here
            *
            * its like doInBackground()
            * */

            try {
//                val externalRef = "914b9a20-950b-44f7-bd7b-d81d57992294" // gregoire
//                val externalRef = "044209a1-8331-4bdc-9a73-8eebbe0acdaa" // gregoire-android
//                val externalRef = "random-user-android"
//                val externalRef = "android-user"
                val externalRef = "gregoire-android"

                Log.d("MobilyFlow", "Go login ")
                customer = mobily!!.login(externalRef)
                Log.d("MobilyFlow", "isForwardingEnable (customer): " + (customer!!.isForwardingEnable))
                Log.d("MobilyFlow", "isForwardingEnable (direct): " + (mobily!!.isForwardingEnable(externalRef)))

                val products = mobily!!.getProducts(null, false)

                Log.d("MobilyFlow", "External Entitlements: ")
                val entitlements = mobily!!.getExternalEntitlements()
                for (entitlement in entitlements) {
                    Log.d("MobilyFlow", "    ${entitlement.product.identifier} / ${entitlement.customerId}")
                }
                Log.d("MobilyFlow", "==================")

                for (product in products) {
                    Log.d("AppForge", "Product identifier: ${product.id} / ${product.type}")
                }
//                val groups = mobily!!.getSubscriptionGroups(arrayOf("forge_premium"), false)
//                Log.d("MobilyFlow", "Go products")
//                val products = groups[0].products

                /*for (group in groups) {
                    Log.d("AppForge", "Group ${group.identifier}")
                    for (product in group.products) {
                        Log.d("AppForge", "Product android_sku: ${product.android_basePlanId}")
                    }
                }*/
                /*for (product in products) {
                    Log.d("AppForge", "Product id: ${product.id}")
                    Log.d("AppForge", "Product createdAt: ${product.createdAt}")
                    Log.d("AppForge", "Product updatedAt: ${product.updatedAt}")
                    Log.d("AppForge", "Product identifier: ${product.identifier}")
                    Log.d("AppForge", "Product appId: ${product.appId}")
                    Log.d("AppForge", "Product name: ${product.name}")
                    Log.d("AppForge", "Product description: ${product.description}")
                    Log.d("AppForge", "Product android_sku: ${product.android_sku}")
                    Log.d(
                        "AppForge",
                        "Product android_basePlanId: ${product.android_basePlanId}"
                    )
                    Log.d("AppForge", "Product type: ${product.type}")
                    Log.d("AppForge", "Product extras: ${product.extras}")

                    Log.d("AppForge", "Product price: ${product.price}")
                    Log.d("AppForge", "Product currency: ${product.currencyCode}")
                    Log.d("AppForge", "Product priceFormatted: ${product.priceFormatted}")
                    Log.d("AppForge", "Product status: ${product.status}")

                    if (product.oneTimeDetails != null) {
                        Log.d(
                            "AppForge",
                            "Product isConsumable: ${product.oneTimeDetails.isConsumable}"
                        )
                        Log.d(
                            "AppForge",
                            "Product isMultiQuantity: ${product.oneTimeDetails.isMultiQuantity}"
                        )
                    } else {
                        Log.d(
                            "AppForge",
                            "ProductperiodCount: ${product.subscriptionDetails!!.periodCount}"
                        )
                        Log.d(
                            "AppForge",
                            "Product periodUnit: ${product.subscriptionDetails.periodUnit}"
                        )
                    }

                    Log.d("AppForge", "====================")
                }*/
                this.products.postValue(products)
            } catch (e: MobilyException) {
                this.error.postValue(e.type.toString())
                Log.e("MobilyFlow", "Error: ${e.type} ${e.message}")
            }

//            forge.playground()

            handler.post {
                /*
                * You can perform any operation that
                * requires UI Thread here.
                *
                * its like onPostExecute()
                * */
            }
        }
    }
}

class PurchaseProductHelper(
    val activity: Activity,
    val customerId: String,
    val sku: String,
    val basePlanId: String? = null
) :
    BillingClientStateListener, PurchasesUpdatedListener {
    val client: BillingClient

    init {
        client = BillingClient.newBuilder(activity)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        client.startConnection(this)
    }

    override fun onBillingServiceDisconnected() {
        Log.d("MobilyFlow", "onBillingServiceDisconnected")
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        Log.d("MobilyFlow", "onBillingSetupFinished ${result.responseCode} ${result.debugMessage}")
        val products = arrayListOf<QueryProductDetailsParams.Product>()

        val queryBuilder = QueryProductDetailsParams.Product.newBuilder()
        queryBuilder.setProductId(this.sku)
        queryBuilder.setProductType(if (basePlanId != null) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP)
        products.add(queryBuilder.build())

        val request = QueryProductDetailsParams.newBuilder().setProductList(products).build()

        this.client.queryProductDetailsAsync(request) { billingResult, productDetails ->
            if (productDetails.size == 0) {
                Log.e("MobilyFlow", "Product not found")
            } else {

                Log.d("MobilyFlow", "${productDetails[0].productId} ${productDetails[0].productType}")
                val productDetailBuilder =
                    BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails[0])

                if (productDetails[0].productType == "subs") {
                    val basePlan =
                        productDetails[0].subscriptionOfferDetails!!.find { x -> x.basePlanId == basePlanId && x.offerId == null }
                    if (basePlan == null) {
                        Log.e("MobilyFlow", "Base plan not found")
                        return@queryProductDetailsAsync
                    } else {
                        productDetailBuilder.setOfferToken(basePlan.offerToken)
                    }
                }

                val builder =
                    BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productDetailBuilder.build()))

                builder.setObfuscatedAccountId(customerId)

                client.launchBillingFlow(activity, builder.build())
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (purchases != null) {
            for (purchase in purchases) {
                if (basePlanId != null) {
                    client.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    ) { billingResult ->
                        Logger.d("finishPurchase: acknowledgePurchase result: ${billingResult.responseCode}/${billingResult.debugMessage}")
                    }
                } else {
                    client.consumeAsync(
                        ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    ) { billingResult, purchaseToken ->
                        Logger.d("finishPurchase: consumeAsync result: ${billingResult.responseCode}/${billingResult.debugMessage}, purchaseToken: $purchaseToken")
                    }
                }
            }
        }
    }
}

@Composable
fun IAPButton(
    activity: Activity,
    sdk: MobilyPurchaseSDK,
    product: MobilyProduct,
    offer: MobilySubscriptionOffer?,
) {
    Button(onClick = {
        Executors.newSingleThreadExecutor().execute {
            try {
                if (offer == null) {
                    Log.d("MobilyFlow", "Click ${product.identifier}")
                    val status = sdk.purchaseProduct(activity, product)
                    Log.d("MobilyFlow", "Purchase result = $status")
                } else {
                    Log.d(
                        "MobilyFlow",
                        "Click ${product.identifier} offer ${offer.android_offerId ?: "null"}"
                    )
                    val status = sdk.purchaseProduct(activity, product, PurchaseOptions().setOffer(offer))
                    Log.d("MobilyFlow", "Purchase result = $status")
                }
            } catch (e: MobilyPurchaseException) {
                Log.e(
                    "MobilyFlow",
                    "purchaseProduct error: ${e.type}",
                )
            } catch (e: MobilyException) {
                Log.e(
                    "MobilyFlow",
                    "purchaseProduct error: ${e.type}",
                )
            } catch (e: Exception) {
                Log.e(
                    "MobilyFlow",
                    "purchaseProduct error: ",
                    e
                )
            }
        }
    }, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(product.name)
            Text(product.description)
            Text(product.identifier)
            Text(
                offer?.priceFormatted ?: product.oneTimeProduct?.priceFormatted
                ?: product.subscriptionProduct?.baseOffer?.priceFormatted ?: "-"
            )
            if (offer != null) {
                Text(offer.android_offerId ?: "-")
                Text("${offer.periodCount} ${offer.periodUnit} - ${offer.countBillingCycle}/cycles")
            }
            Text("Status = " + (offer?.status ?: product.status).toString())
        }
    }
}
