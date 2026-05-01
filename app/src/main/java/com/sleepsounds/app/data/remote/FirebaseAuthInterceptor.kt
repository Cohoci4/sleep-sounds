package com.sleepsounds.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * Attaches `Authorization: Bearer <Firebase ID token>` to every outbound
 * request the app makes to its own backend so the Cloud Functions can
 * verify the caller's identity instead of trusting any `userId` in the
 * request body.
 *
 * The interceptor is best-effort: if no Firebase user is signed in or the
 * token mint fails the request goes out without the header and the server
 * will reject it with `401 unauthenticated`. We never silently swap in a
 * different user's identity.
 */
@Singleton
class FirebaseAuthInterceptor @Inject constructor(
    private val firebaseAuth: Lazy<FirebaseAuth>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Don't overwrite a header the caller has explicitly set.
        if (request.header("Authorization") != null) {
            return chain.proceed(request)
        }
        val idToken = currentIdToken()
        return if (idToken == null) {
            chain.proceed(request)
        } else {
            chain.proceed(
                request.newBuilder()
                    .header("Authorization", "Bearer $idToken")
                    .build()
            )
        }
    }

    private fun currentIdToken(): String? {
        val user = runCatching { firebaseAuth.get().currentUser }.getOrNull() ?: return null
        return runCatching {
            // `getIdToken(false)` reuses the cached token until it is close
            // to expiry, then refreshes it. The OkHttp dispatcher already
            // runs on a background thread so blocking here is acceptable.
            runBlocking { user.getIdToken(false).await().token }
        }.onFailure { Timber.w(it, "Firebase ID token mint failed") }.getOrNull()
    }
}
