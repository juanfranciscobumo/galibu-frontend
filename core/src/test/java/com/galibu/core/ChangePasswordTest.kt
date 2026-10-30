package com.galibu.core

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ChangePasswordTest : BaseRepositoryTest() {

    private fun httpError(code: Int, body: String? = null): HttpException {
        val ex = mockk<HttpException>()
        every { ex.code() } returns code
        if (body == null) {
            every { ex.response() } returns null
        } else {
            val response = mockk<Response<Any>>()
            val errorBody = mockk<ResponseBody>()
            every { response.errorBody() } returns errorBody
            every { errorBody.string() } returns body
            every { ex.response() } returns response
        }
        return ex
    }

    @Test
    fun `changePassword succeeds for local account`() = runTest {
        coEvery { mockApi.changePassword(any()) } just Runs

        val error = repository.changePassword("actual123", "nueva1234")

        assertNull(error)
        coVerify(exactly = 1) { mockApi.changePassword(any()) }
    }

    @Test
    fun `changePassword rejects short password before calling backend`() = runTest {
        val error = repository.changePassword("actual123", "corta")

        assertTrue(error != null && error.contains("8 caracteres"))
        coVerify(exactly = 0) { mockApi.changePassword(any()) }
    }

    @Test
    fun `changePassword maps wrong current password`() = runTest {
        coEvery { mockApi.changePassword(any()) } throws httpError(401)

        val error = repository.changePassword("incorrecta", "nueva1234")

        assertEquals("La contraseña actual es incorrecta", error)
    }

    @Test
    fun `changePassword shows backend message for Google account`() = runTest {
        val body = """{"status":409,"message":"Tu cuenta está vinculada a Google. Cambia tu contraseña desde tu cuenta de Google."}"""
        coEvery { mockApi.changePassword(any()) } throws httpError(409, body)

        val error = repository.changePassword("loQueSea", "nueva1234")

        assertTrue(error != null && error.contains("vinculada a Google"))
    }
}
