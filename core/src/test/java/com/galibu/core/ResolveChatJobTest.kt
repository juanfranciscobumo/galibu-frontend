package com.galibu.core

import com.galibu.core.data.model.UserProfile
import com.galibu.core.data.remote.CreateJobDto
import com.galibu.core.data.repository.GalibuRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveChatJobTest : BaseRepositoryTest() {

    private fun setCurrentUserDirect(user: UserProfile?) {
        val field = GalibuRepository::class.java.getDeclaredField("_currentUser")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(repository) as MutableStateFlow<UserProfile?>).value = user
    }

    @Test
    fun `resolveChatJobId reuses explicit job from Room`() = runTest {
        fakeJobDao.insertJob(createJob(id = "job_1", status = "NEGOTIATING", clientId = "client_1"))

        val result = repository.resolveChatJobId("pro_1", "Pro", "Plomería", jobId = "job_1")

        assertEquals("job_1", result)
        assertEquals(1, fakeJobDao.getAllJobsSnapshot().size)
    }

    @Test
    fun `resolveChatJobId fetches explicit job from backend when missing locally`() = runTest {
        val backendJob = createJob(id = "job_9", status = "OPEN", clientId = "client_1")
        coEvery { mockApi.getJob("job_9") } returns backendJob

        val result = repository.resolveChatJobId("pro_1", "Pro", "Plomería", jobId = "job_9")

        assertEquals("job_9", result)
        assertEquals("job_9", fakeJobDao.getJobByIdSync("job_9")?.id)
    }

    @Test
    fun `resolveChatJobId falls back to Firestore for legacy threads`() = runTest {
        coEvery { mockApi.getJob("chat_old") } throws RuntimeException("404")
        val legacy = createJob(id = "chat_old", status = "NEGOTIATING", clientId = "client_1")
        coEvery { mockFirebase.getJobFromCloud("chat_old") } returns legacy

        val result = repository.resolveChatJobId("pro_1", "Pro", "Plomería", jobId = "chat_old")

        assertEquals("chat_old", result)
        assertEquals("chat_old", fakeJobDao.getJobByIdSync("chat_old")?.id)
    }

    @Test
    fun `resolveChatJobId reuses active job with professional instead of creating`() = runTest {
        setCurrentUserDirect(createUser(id = "client_1", email = "c@test.com", role = "CLIENT"))
        fakeJobDao.insertJob(
            createJob(id = "job_2", status = "NEGOTIATING", clientId = "client_1", professionalId = "pro_1")
        )

        val result = repository.resolveChatJobId("pro_1", "Pro", "Plomería")

        assertEquals("job_2", result)
        assertTrue(fakeJobDao.getAllJobsSnapshot().none { it.id.startsWith("chat_") })
    }

    @Test
    fun `resolveChatJobId ignores CANCELLED jobs and creates a new thread`() = runTest {
        setCurrentUserDirect(createUser(id = "client_1", email = "c@test.com", role = "CLIENT"))
        fakeJobDao.insertJob(
            createJob(id = "job_old", status = "CANCELLED", clientId = "client_1", professionalId = "pro_1")
        )
        val slot = slot<CreateJobDto>()
        coEvery { mockApi.createJob(capture(slot)) } answers {
            createJob(
                id = slot.captured.id ?: "chat_x",
                status = "NEGOTIATING",
                clientId = "client_1",
                professionalId = "pro_1"
            )
        }

        val result = repository.resolveChatJobId("pro_1", "Pro", "Plomería")

        assertTrue(result != null && result.startsWith("chat_pro_1_"))
        assertEquals(result, fakeJobDao.getJobByIdSync(result!!)?.id)
    }

    @Test
    fun `resolveChatJobId returns null when creation needs a user but none signed in`() = runTest {
        val result = repository.resolveChatJobId("pro_1", "Pro", "Plomería")

        assertNull(result)
    }
}
