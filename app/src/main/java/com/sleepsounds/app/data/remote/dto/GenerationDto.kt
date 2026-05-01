package com.sleepsounds.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// The caller's identity is taken server-side from the verified Firebase
// Auth ID token attached as `Authorization: Bearer ...` (see
// FirebaseAuthInterceptor), so request bodies no longer carry `userId`.

data class GenerationRequestDto(
    @SerializedName("prompt") val prompt: String,
)

data class GenerationResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("audioUrl") val audioUrl: String,
    @SerializedName("coverUrl") val coverUrl: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
)

data class SubscriptionVerificationDto(
    @SerializedName("purchaseToken") val purchaseToken: String,
    @SerializedName("productId") val productId: String,
)
