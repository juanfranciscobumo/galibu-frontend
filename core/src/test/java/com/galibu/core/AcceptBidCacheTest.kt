package com.galibu.core

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AcceptBidCacheTest : BaseRepositoryTest() {

    @Test
    fun `acceptBid keeps the accepted bid in local cache`() = runTest {
        fakeJobDao.insertJob(createJob(id = "job_1", status = "NEGOTIATING", clientId = "client_1"))
        fakeBidDao.insertBid(createBid(id = "bid_1", jobId = "job_1", professionalId = "pro_1", status = "PENDING"))

        val acceptedBid = createBid(id = "bid_1", jobId = "job_1", professionalId = "pro_1", status = "ACCEPTED")
        val acceptedJob = createJob(id = "job_1", status = "ACCEPTED", clientId = "client_1", professionalId = "pro_1")
        coEvery { mockApi.acceptBid("bid_1") } returns acceptedBid
        coEvery { mockApi.getJob("job_1") } returns acceptedJob
        coEvery { mockApi.jobBids("job_1") } returns listOf(acceptedBid)

        repository.acceptBid("bid_1")

        assertEquals("ACCEPTED", fakeBidDao.getBidByIdSync("bid_1")?.status)
        assertEquals("ACCEPTED", fakeJobDao.getJobByIdSync("job_1")?.status)
        assertEquals("pro_1", fakeJobDao.getJobByIdSync("job_1")?.professionalId)
    }
}
