package com.galibu.core

import com.galibu.core.data.model.UserProfile
import com.galibu.core.data.repository.GalibuRepository
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalibuRepositoryTest : BaseRepositoryTest() {

    private fun setCurrentUser(user: UserProfile) {
        val currentUserField = GalibuRepository::class.java.getDeclaredField("_currentUser")
        currentUserField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val currentUserFlow = currentUserField.get(repository) as MutableStateFlow<UserProfile?>
        currentUserFlow.value = user
    }

    @Test
    fun `cancelJobRequest deletes local job`() = runTest {
        fakeJobDao.insertJob(createJob(id = "job_1"))

        repository.cancelJobRequest("job_1")

        assertNull(fakeJobDao.getJobByIdSync("job_1"))
        coVerify { mockApi.deleteJob("job_1") }
    }

    @Test
    fun `hireProfessional reuses active job instead of creating`() = runTest {
        val active = createJob(
            id = "job_active",
            status = "NEGOTIATING",
            clientId = "client_1",
            professionalId = "pro_1"
        )
        coEvery { mockApi.jobs(any(), any(), any()) } returns listOf(active)
        setCurrentUser(createUser(id = "client_1", email = "c@t.com", role = "CLIENT"))

        val result = repository.hireProfessional("pro_1", "Pro", "Plomería", 40000.0, null)

        assertEquals("job_active", result)
        assertEquals("job_active", fakeJobDao.getJobByIdSync("job_active")?.id)
    }
}
