package com.sleepsounds.app.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.sleepsounds.app.BuildConfig
import com.sleepsounds.app.data.remote.api.SleepSoundsApi
import com.sleepsounds.app.data.remote.dto.SubscriptionVerificationDto
import com.sleepsounds.app.di.IoDispatcher
import com.sleepsounds.app.domain.model.SubscriptionStatus
import com.sleepsounds.app.domain.model.SubscriptionTier
import com.sleepsounds.app.domain.repository.AuthRepository
import com.sleepsounds.app.domain.repository.SubscriptionProduct
import com.sleepsounds.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: dagger.Lazy<SleepSoundsApi>,
    private val authRepository: AuthRepository,
    private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SubscriptionRepository, PurchasesUpdatedListener {

    private val _status = MutableStateFlow(SubscriptionStatus(SubscriptionTier.FREE))
    private val _products = MutableStateFlow<List<SubscriptionProduct>>(emptyList())

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    private var connected = false

    private val statusFlow = _status.asStateFlow().stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = SubscriptionStatus(SubscriptionTier.FREE),
    )

    init {
        applicationScope.launch { connectAndQuery() }
    }

    override fun observeStatus(): Flow<SubscriptionStatus> = statusFlow

    override fun observeProducts(): Flow<List<SubscriptionProduct>> = _products

    override suspend fun launchPurchaseFlow(
        activity: Activity,
        productId: String,
        basePlanId: String?,
    ): Result<Unit> = runCatching {
        // Network/IO setup happens off the main thread...
        val params = withContext(ioDispatcher) {
            ensureConnected()
            val productDetails = queryProductDetails(productId)
                ?: error("Product $productId not found")
            val offerToken = productDetails.subscriptionOfferDetails
                ?.firstOrNull { basePlanId == null || it.basePlanId == basePlanId }
                ?.offerToken
                ?: error("No offer token for $productId")
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .build()
        }
        // ...but launchBillingFlow must run on the main thread (it shows UI).
        val result = withContext(Dispatchers.Main.immediate) {
            billingClient.launchBillingFlow(activity, params)
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            error("launchBillingFlow failed: ${result.debugMessage}")
        }
    }

    override suspend fun restorePurchases(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            ensureConnected()
            val purchases = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            handlePurchases(purchases.purchasesList)
        }
    }

    override suspend fun verifyPurchaseToken(token: String, productId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                if (!BuildConfig.FIREBASE_ENABLED) return@runCatching
                val userId = authRepository.ensureSignedIn().getOrThrow()
                val response = api.get().checkSubscription(
                    SubscriptionVerificationDto(userId, token, productId)
                )
                _status.value = SubscriptionStatus(
                    tier = runCatching { SubscriptionTier.valueOf(response.tier) }
                        .getOrDefault(SubscriptionTier.FREE),
                    expiresAtEpochMs = response.expiresAtEpochMs,
                    isAutoRenewing = response.isAutoRenewing,
                    inGracePeriod = response.inGracePeriod,
                    onAccountHold = response.onAccountHold,
                )
            }.onFailure { Timber.w(it, "verifyPurchaseToken failed") }
        }

    private suspend fun connectAndQuery() {
        runCatching {
            ensureConnected()
            queryAndPublishProducts()
            val existing = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            handlePurchases(existing.purchasesList)
        }.onFailure { Timber.w(it, "Billing init failed") }
    }

    private suspend fun ensureConnected() {
        if (connected && billingClient.isReady) return
        suspendCancellableCoroutine<Unit> { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    connected = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    if (cont.isActive) cont.resume(Unit)
                }
                override fun onBillingServiceDisconnected() { connected = false }
            })
        }
    }

    private suspend fun queryProductDetails(productId: String): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        return billingClient.queryProductDetails(params).productDetailsList?.firstOrNull()
    }

    private suspend fun queryAndPublishProducts() {
        val ids = listOf(PRODUCT_MONTHLY, PRODUCT_YEARLY)
        val productList = ids.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val result = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        )
        val products = result.productDetailsList.orEmpty().flatMap { details ->
            details.subscriptionOfferDetails.orEmpty().map { offer ->
                val phase = offer.pricingPhases.pricingPhaseList.firstOrNull()
                SubscriptionProduct(
                    productId = details.productId,
                    basePlanId = offer.basePlanId,
                    title = details.title,
                    description = details.description,
                    formattedPrice = phase?.formattedPrice.orEmpty(),
                    billingPeriod = phase?.billingPeriod.orEmpty(),
                )
            }
        }
        _products.value = products
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.d("PurchasesUpdated: ${billingResult.responseCode} ${billingResult.debugMessage}")
            return
        }
        applicationScope.launch { handlePurchases(purchases.orEmpty()) }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            val productId = purchase.products.firstOrNull() ?: continue
            verifyPurchaseToken(purchase.purchaseToken, productId)
            if (!purchase.isAcknowledged) {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                )
            }
        }
    }

    companion object {
        const val PRODUCT_MONTHLY = "sleep_premium_monthly"
        const val PRODUCT_YEARLY = "sleep_premium_yearly"
    }
}
