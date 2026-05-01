package com.sleepsounds.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeUserId(): Flow<String?>
    suspend fun ensureSignedIn(): Result<String>
    suspend fun signOut()
}
