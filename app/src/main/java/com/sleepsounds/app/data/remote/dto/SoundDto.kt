package com.sleepsounds.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SoundDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("audioUrl") val audioUrl: String,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("tier") val tier: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("createdAtEpochMs") val createdAtEpochMs: Long? = null,
)
