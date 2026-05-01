package com.sleepsounds.app.data.remote.api

import com.sleepsounds.app.data.remote.dto.GenerationRequestDto
import com.sleepsounds.app.data.remote.dto.GenerationResponseDto
import com.sleepsounds.app.data.remote.dto.SubscriptionVerificationDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * REST shape exposed by the Firebase Cloud Functions backend. Each function is
 * deployed as an HTTPS endpoint via `onRequest`. See `functions/src/index.ts`.
 */
interface SleepSoundsApi {
    @POST("generateSound")
    suspend fun generateSound(@Body request: GenerationRequestDto): GenerationResponseDto

    @POST("checkSubscription")
    suspend fun checkSubscription(@Body request: SubscriptionVerificationDto): SubscriptionStatusResponse
}

data class SubscriptionStatusResponse(
    val tier: String,
    val expiresAtEpochMs: Long?,
    val isAutoRenewing: Boolean,
    val inGracePeriod: Boolean,
    val onAccountHold: Boolean,
)
