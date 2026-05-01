package com.sleepsounds.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GenerationRequestDto(
    @SerializedName("prompt") val prompt: String,
    @SerializedName("userId") val userId: String,
)

data class GenerationResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("audioUrl") val audioUrl: String,
    @SerializedName("coverUrl") val coverUrl: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
)

data class SubscriptionVerificationDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("purchaseToken") val purchaseToken: String,
    @SerializedName("productId") val productId: String,
)
