package com.galibu.core

import com.galibu.core.data.remote.AuthResponseDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

class AuthFlowTest : BaseRepositoryTest() {

    private fun tokens(user: com.galibu.core.data.model.UserProfile) =
        AuthResponseDto("access-1", "refresh-1", "Bearer", 3600L, user)

    private fun http409(): HttpException {
        val ex = mockk<HttpException>()
        every { ex.code() } returns 409
        return ex
    }

    @Test
    fun `loginWithGoogle stores tokens and sets user`() = runTest {
        val user = createUser(id = "g1", email = "g@test.com", role = "PROFESSIONAL")
        coEvery { mockApi.google(any()) } returns tokens(user)
        coEvery { mockApi.updateMe(any()) } returns user

        val ok = repository.loginWithGoogle(
            idToken = "google-token",
            email = "g@test.com",
            name = "G",
            avatarUrl = null,
            role = "PROFESSIONAL"
        )

        assertTrue(ok)
        assertEquals("g1", repository.currentUser.value?.id)
        assertTrue(tokenStore.hasTokens())
    }

    @Test
    fun `loginWithGoogle refuses blank token`() = runTest {
        val ok = repository.loginWithGoogle(
            idToken = null,
            email = "g@test.com",
            name = "G",
            avatarUrl = null,
            role = "PROFESSIONAL"
        )

        assertFalse(ok)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun `login maps 401 to credentials message`() = runTest {
        val ex = mockk<HttpException>()
        every { ex.code() } returns 401
        coEvery { mockApi.login(any()) } throws ex

        val msg = repository.login("a@test.com", "wrong", "CLIENT")

        assertTrue(msg != null && msg.contains("incorrectos"))
        assertTrue(!tokenStore.hasTokens())
    }

    @Test
    fun `login rejects role mismatch and clears tokens`() = runTest {
        val user = createUser(id = "u1", email = "a@test.com", role = "PROFESSIONAL")
        coEvery { mockApi.login(any()) } returns tokens(user)
        tokenStore.saveTokens("a", "r")

        val msg = repository.login("a@test.com", "secreto123", "CLIENT")

        assertTrue(msg != null && msg.contains("rol"))
        assertTrue(!tokenStore.hasTokens())
        assertNull(repository.currentUser.value)
    }

    @Test
    fun `register with google token creates session`() = runTest {
        val user = createUser(id = "g2", email = "n@test.com", role = "PROFESSIONAL")
        coEvery { mockApi.google(any()) } returns tokens(user)
        coEvery { mockApi.updateMe(any()) } returns user

        repository.registerUser(
            context = mockk(relaxed = true),
            name = "Nuevo",
            email = "n@test.com",
            phone = "+57300111",
            password = "",
            role = "PROFESSIONAL",
            profilePhotoUrl = "data:image/png;base64,AAA",
            googleIdToken = "google-token"
        )

        assertEquals("g2", repository.currentUser.value?.id)
        assertTrue(tokenStore.hasTokens())
    }

    @Test
    fun `register conflict with opposite role expands to BOTH`() = runTest {
        val existing = createUser(id = "e1", email = "e@test.com", role = "CLIENT")
        coEvery { mockApi.register(any()) } throws http409()
        coEvery { mockApi.login(any()) } returns tokens(existing)
        val expanded = createUser(id = "e1", email = "e@test.com", role = "BOTH")
        coEvery { mockApi.addRole(any()) } returns expanded
        coEvery { mockApi.getMe() } returns expanded
        coEvery { mockApi.updateMe(any()) } returns expanded

        repository.registerUser(
            context = mockk(relaxed = true),
            name = "E",
            email = "e@test.com",
            phone = "+57300222",
            password = "secreto123",
            role = "PROFESSIONAL",
            profilePhotoUrl = "data:image/png;base64,AAA"
        )

        assertEquals("BOTH", repository.currentUser.value?.role)
    }

    @Test
    fun `register conflict with same role fails with EMAIL_EXISTS`() = runTest {
        val existing = createUser(id = "e1", email = "e@test.com", role = "CLIENT")
        coEvery { mockApi.register(any()) } throws http409()
        coEvery { mockApi.login(any()) } returns tokens(existing)

        var message: String? = null
        try {
            repository.registerUser(
                context = mockk(relaxed = true),
                name = "E",
                email = "e@test.com",
                phone = "+57300333",
                password = "secreto123",
                role = "CLIENT"
            )
        } catch (e: IllegalStateException) {
            message = e.message
        }

        assertTrue(message != null && message.contains("EMAIL_EXISTS"))
        assertNull(repository.currentUser.value)
    }
}
