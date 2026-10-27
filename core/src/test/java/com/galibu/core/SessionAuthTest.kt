package com.galibu.core

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAuthTest : BaseRepositoryTest() {

    @Test
    fun `restoreSession returns false without stored tokens`() = runTest {
        assertFalse(repository.restoreSession())
        assertNull(repository.currentUser.value)
    }

    @Test
    fun `restoreSession restores user from backend`() = runTest {
        tokenStore.saveTokens("access-1", "refresh-1")
        val user = createUser(id = "u1", email = "u1@test.com")
        coEvery { mockApi.getMe() } returns user

        assertTrue(repository.restoreSession())

        assertEquals("u1", repository.currentUser.value?.id)
        assertEquals("u1@test.com", fakeUserDao.getUserByIdSync("u1")?.email)
    }

    @Test
    fun `restoreSession returns false when backend rejects`() = runTest {
        tokenStore.saveTokens("bad-access", "bad-refresh")
        coEvery { mockApi.getMe() } throws RuntimeException("401")

        assertFalse(repository.restoreSession())
        assertNull(repository.currentUser.value)
    }

    @Test
    fun `refreshJobs merges backend jobs and deletes stale ones`() = runTest {
        tokenStore.saveTokens("access-1", "refresh-1")
        val user = createUser(id = "u1", email = "u1@test.com", role = "CLIENT")
        coEvery { mockApi.getMe() } returns user
        coEvery { mockApi.jobs(any(), any(), any()) } returns listOf(
            createJob(id = "job_new", status = "OPEN", clientId = "u1")
        )
        repository.restoreSession()

        fakeJobDao.insertJob(createJob(id = "stale_job", status = "OPEN", clientId = "u1"))

        repository.refreshJobs()

        assertEquals("job_new", fakeJobDao.getJobByIdSync("job_new")?.id)
        assertNull(fakeJobDao.getJobByIdSync("stale_job"))
    }

    @Test
    fun `refreshBids stores professional bids from backend`() = runTest {
        tokenStore.saveTokens("access-1", "refresh-1")
        val user = createUser(id = "pro_1", email = "pro@test.com", role = "PROFESSIONAL")
        coEvery { mockApi.getMe() } returns user
        repository.restoreSession()

        coEvery { mockApi.myBids() } returns listOf(
            createBid(id = "bid_1", jobId = "job_1", professionalId = "pro_1")
        )

        repository.refreshBids()

        assertEquals("bid_1", fakeBidDao.getBidByIdSync("bid_1")?.id)
    }
}
