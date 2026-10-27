package com.galibu.core

import com.galibu.core.data.remote.InMemoryTokenStorage
import com.galibu.core.data.remote.TokenAuthenticator
import com.galibu.core.data.remote.TokenStore
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenSessionTest {

    private fun request(url: String = "http://localhost:8080/api/jobs"): Request {
        return Request.Builder().url(url).build()
    }

    private fun denied(request: Request, prior: Response? = null): Response {
        val builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
        if (prior != null) {
            builder.priorResponse(prior)
        }
        return builder.build()
    }

    @Test
    fun `TokenStore roundtrip and clear`() {
        val store = TokenStore(InMemoryTokenStorage())

        assertTrue(!store.hasTokens())
        store.saveTokens("a1", "r1")
        assertTrue(store.hasTokens())
        assertEquals("a1", store.accessToken())
        assertEquals("r1", store.refreshToken())
        store.clear()
        assertTrue(!store.hasTokens())
        assertNull(store.accessToken())
    }

    @Test
    fun `authenticator returns null without refresh token`() {
        val store = TokenStore(InMemoryTokenStorage())
        val auth = TokenAuthenticator(store) { null }

        assertNull(auth.authenticate(null, denied(request())))
    }

    @Test
    fun `authenticator refreshes once and retries with new token`() {
        val store = TokenStore(InMemoryTokenStorage())
        store.saveTokens("old-access", "good-refresh")
        val auth = TokenAuthenticator(store) { refresh ->
            if (refresh == "good-refresh") {
                com.galibu.core.data.remote.AuthResponseDto(
                    accessToken = "new-access",
                    refreshToken = "new-refresh"
                )
            } else {
                null
            }
        }

        val retry = auth.authenticate(null, denied(request()))

        assertTrue(retry != null)
        assertEquals("Bearer new-access", retry!!.header("Authorization"))
        assertEquals("new-access", store.accessToken())
        assertEquals("new-refresh", store.refreshToken())
    }

    @Test
    fun `authenticator gives up when refresh fails and clears tokens`() {
        val store = TokenStore(InMemoryTokenStorage())
        store.saveTokens("old-access", "bad-refresh")
        val auth = TokenAuthenticator(store) { null }

        assertNull(auth.authenticate(null, denied(request())))
        assertTrue(!store.hasTokens())
    }

    @Test
    fun `authenticator does not loop on repeated 401`() {
        val store = TokenStore(InMemoryTokenStorage())
        store.saveTokens("a", "r")
        var calls = 0
        val auth = TokenAuthenticator(store) {
            calls++
            com.galibu.core.data.remote.AuthResponseDto(accessToken = "n", refreshToken = "n2")
        }

        val first = denied(request())
        val second = denied(request(), first)

        assertNull(auth.authenticate(null, second))
        assertEquals(0, calls)
    }
}
