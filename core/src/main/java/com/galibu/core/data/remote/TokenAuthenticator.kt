package com.galibu.core.data.remote

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * On 401, tries a single synchronous refresh-token exchange and retries the
 * failed request once with the new access token. Gives up (and clears stored
 * tokens) when there is no refresh token, the refresh fails, or a retry loop
 * is detected.
 *
 * The [refreshCall] receives the stored refresh token and must return fresh
 * tokens, or null/throw when the session can no longer be renewed. It runs on
 * OkHttp's background thread, so blocking calls are allowed.
 */
class TokenAuthenticator(
    private val store: TokenStore,
    private val refreshCall: (String) -> AuthResponseDto?
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            return null
        }
        val refreshToken = store.refreshToken() ?: return null
        // Don't try to refresh the refresh call itself.
        if (response.request.url.encodedPath.endsWith("/auth/refresh")) {
            return null
        }
        val fresh = try {
            refreshCall(refreshToken)
        } catch (e: Exception) {
            null
        }
        if (fresh == null) {
            store.clear()
            return null
        }
        store.saveTokens(fresh.accessToken, fresh.refreshToken)
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${fresh.accessToken}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
