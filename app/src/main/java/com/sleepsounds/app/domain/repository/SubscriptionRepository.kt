package com.sleepsounds.app.domain.repository

import android.app.Activity
import com.sleepsounds.app.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun observeStatus(): Flow<SubscriptionStatus>

    /** Streams the available subscription products as priced via Play Billing. */
    fun observeProducts(): Flow<List<SubscriptionProduct>>

    suspend fun launchPurchaseFlow(activity: Activity, productId: String, basePlanId: String? = null): Result<Unit>

    suspend fun restorePurchases(): Result<Unit>

    suspend fun verifyPurchaseToken(token: String, productId: String): Result<Unit>
}

data class SubscriptionProduct(
    val productId: String,
    val basePlanId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val billingPeriod: String,
)
