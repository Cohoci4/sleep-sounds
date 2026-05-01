package com.sleepsounds.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.sleepsounds.app.BuildConfig
import com.sleepsounds.app.di.IoDispatcher
import com.sleepsounds.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authProvider: dagger.Lazy<FirebaseAuth>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override fun observeUserId(): Flow<String?> {
        if (!BuildConfig.FIREBASE_ENABLED) return flowOf(LOCAL_USER_ID)
        return callbackFlow {
            val auth = authProvider.get()
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }

    override suspend fun ensureSignedIn(): Result<String> = withContext(ioDispatcher) {
        if (!BuildConfig.FIREBASE_ENABLED) return@withContext Result.success(LOCAL_USER_ID)
        runCatching {
            val auth = authProvider.get()
            auth.currentUser?.uid ?: auth.signInAnonymously().await().user?.uid
                ?: error("Anonymous sign-in returned no user")
        }.onFailure { Timber.w(it, "ensureSignedIn failed") }
    }

    override suspend fun signOut() {
        if (BuildConfig.FIREBASE_ENABLED) {
            authProvider.get().signOut()
        }
    }

    companion object {
        private const val LOCAL_USER_ID = "local"
    }
}
