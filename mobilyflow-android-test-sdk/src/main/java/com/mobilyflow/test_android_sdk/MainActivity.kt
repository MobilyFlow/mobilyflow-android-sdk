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
import com.mobilyflow.mobilypurchasesdk.Enums.MobilyEnvironment
import com.mobilyflow.mobilypurchasesdk.Enums.ProductType
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyPurchaseException
import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyTransferOwnershipException
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseSDK
import com.mobilyflow.mobilypurchasesdk.MobilyPurchaseSDKOptions
import com.mobilyflow.mobilypurchasesdk.Models.MobilyProduct
import com.mobilyflow.mobilypurchasesdk.Models.MobilySubscriptionOffer
import com.mobilyflow.mobilypurchasesdk.Models.PurchaseOptions
import com.mobilyflow.test_android_sdk.ui.theme.MobilyflowAndroidSDKTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private var mobily: MobilyPurchaseSDK? = null

    private var products = MutableLiveData<List<MobilyProduct>?>()
    private var error = MutableLiveData<String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mobily = MobilyPurchaseSDK(
            this,
            appId = "caecc000-45ce-49b3-b218-46c1d985ae85",
            apiKey = "7aa18f9720a5c9731d17f5c54e89bdd218647f71269eed2f6c27c8fa5924da84",
            environment = MobilyEnvironment.DEVELOPMENT,
            options = MobilyPurchaseSDKOptions(
                languages = null,
                debug = true,
                apiURL = "https://mobilyflow.eu-1.sharedwithexpose.com/v1/"
                // apiURL = "https://api-staging.mobilyflow.com/v1/"
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
                                                    horizontal = 10.dp,
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
                val externalRef = "914b9a20-950b-44f7-bd7b-d81d57992294"
//                mobily!!.login("044209a1-8331-4bdc-9a73-8eebbe0acdaa") // gregoire-android (944d6694-1c78-4380-bbce-100634af9428)
                val customer = mobily!!.login(externalRef) // gregoire (4d6d544e-2e08-414a-a29f-799b1022a3d1)
                Log.d("MobilyFlow", "isForwardingEnable (customer): " + (customer.isForwardingEnable))
                Log.d("MobilyFlow", "isForwardingEnable (direct): " + (mobily!!.isForwardingEnable(externalRef)))

//                val products = mobily!!.getProducts(arrayOf("premium-1month", "premium-6month"), false)
                val groups = mobily!!.getSubscriptionGroups(arrayOf("forge_premium"), false)
                val products = groups[0].products

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
                Log.e("MobilyFlow", "Error: ", e)
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
                    sdk.purchaseProduct(activity, product)
                    Log.d("MobilyFlow", "Done")
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
            Text(
                offer?.priceFormatted ?: product.oneTimeProduct?.priceFormatted
                ?: product.subscriptionProduct?.baseOffer?.priceFormatted ?: "-"
            )
            if (product.subscriptionProduct != null) {
                Text(product.subscriptionProduct!!.android_basePlanId)
            }
            if (offer != null) {
                Text(offer.android_offerId ?: "-")
            }
            Text("Status = " + product.status.toString())
        }
    }
}
