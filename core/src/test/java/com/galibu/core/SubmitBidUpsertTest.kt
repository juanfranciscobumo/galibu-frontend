package com.galibu.core

import com.galibu.core.data.model.UserProfile
import com.galibu.core.data.repository.GalibuRepository
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitBidUpsertTest : BaseRepositoryTest() {

    private fun setCurrentUserDirect(user: UserProfile?) {
        val field = GalibuRepository::class.java.getDeclaredField("_currentUser")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(repository) as MutableStateFlow<UserProfile?>).value = user
    }

    private fun proBids(jobId: String, proId: String) =
        fakeBidDao.getBidsSnapshot().filter { it.jobId == jobId && it.professionalId == proId }

    @Test
    fun `submitBid stores backend bid and refreshes job`() = runTest {
        setCurrentUserDirect(createUser(id = "pro_1", email = "pro@test.com", role = "PROFESSIONAL"))
        val apiBid = createBid(id = "bid_job_1_pro_1", jobId = "job_1", professionalId = "pro_1", status = "PENDING")
        val negJob = createJob(id = "job_1", status = "NEGOTIATING", clientId = "client_1")
        coEvery { mockApi.submitBid(any(), any()) } returns apiBid
        coEvery { mockApi.getJob("job_1") } returns negJob
        coEvery { mockApi.jobBids("job_1") } returns listOf(apiBid)

        repository.submitBid("job_1", 60000.0, "Puedo hacerlo rápido", 2)

        val bids = proBids("job_1", "pro_1")
        assertEquals(1, bids.size)
        assertEquals("bid_job_1_pro_1", bids[0].id)
        assertEquals("NEGOTIATING", fakeJobDao.getJobByIdSync("job_1")?.status)
        coVerify { mockFirebase.pushBidToCloud("job_1", apiBid) }
    }

    @Test
    fun `submitBid propagates backend failure without local writes`() = runTest {
        setCurrentUserDirect(createUser(id = "pro_1", email = "pro@test.com", role = "PROFESSIONAL"))
        coEvery { mockApi.submitBid(any(), any()) } throws RuntimeException("500")

        var failed = false
        try {
            repository.submitBid("job_1", 60000.0, "X", 2)
        } catch (e: Exception) {
            failed = true
        }

        assertTrue(failed)
        assertTrue(proBids("job_1", "pro_1").isEmpty())
    }

    @Test
    fun `submitBid from another professional does not touch existing bids`() = runTest {
        setCurrentUserDirect(createUser(id = "pro_2", email = "pro2@test.com", role = "PROFESSIONAL"))
        fakeBidDao.insertBid(createBid(id = "bid_job_1_pro_1", jobId = "job_1", professionalId = "pro_1", status = "PENDING"))
        val apiBid = createBid(id = "bid_job_1_pro_2", jobId = "job_1", professionalId = "pro_2", status = "PENDING")
        coEvery { mockApi.submitBid(any(), any()) } returns apiBid
        coEvery { mockApi.getJob("job_1") } returns createJob(id = "job_1", status = "NEGOTIATING", clientId = "client_1")
        coEvery { mockApi.jobBids("job_1") } returns listOf(apiBid)

        repository.submitBid("job_1", 9000.0, "Mi oferta.", 1)

        assertTrue(fakeBidDao.getBidByIdSync("bid_job_1_pro_1") != null)
        assertTrue(fakeBidDao.getBidByIdSync("bid_job_1_pro_2") != null)
    }
}
