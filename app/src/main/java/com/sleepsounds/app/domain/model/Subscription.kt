package com.sleepsounds.app.domain.model

data class SubscriptionStatus(
    val tier: SubscriptionTier,
    val expiresAtEpochMs: Long? = null,
    val isAutoRenewing: Boolean = false,
    val inGracePeriod: Boolean = false,
    val onAccountHold: Boolean = false,
)

enum class SubscriptionTier { FREE, PREMIUM_MONTHLY, PREMIUM_YEARLY }

val SubscriptionStatus.isPremium: Boolean
    get() = tier == SubscriptionTier.PREMIUM_MONTHLY || tier == SubscriptionTier.PREMIUM_YEARLY
