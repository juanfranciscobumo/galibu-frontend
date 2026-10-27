package com.galibu.core

import com.galibu.core.data.model.Bid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeBidDaoTest {

    private val dao = com.galibu.core.data.fake.FakeBidDao()

    @Test
    fun `insertBid adds bid to empty list`() = runTest {
        val bid = createBid(id = "bid_1")
        dao.insertBid(bid)

        val result = dao.getBidsForJobFlow("job_1").first()
        assertEquals(1, result.size)
    }

    @Test
    fun `insertBid replaces existing bid with same id`() = runTest {
        dao.insertBid(createBid(id = "bid_1", amount = 50000.0))
        dao.insertBid(createBid(id = "bid_1", amount = 75000.0))

        val result = dao.getBidsForJobFlow("job_1").first()
        assertEquals(1, result.size)
        assertEquals(75000.0, result[0].amount, 0.01)
    }

    @Test
    fun `getJobIdsWithBidsFlow returns distinct job IDs`() = runTest {
        dao.insertBid(createBid(id = "bid_1", jobId = "job_1", professionalId = "pro_1"))
        dao.insertBid(createBid(id = "bid_2", jobId = "job_1", professionalId = "pro_1"))
        dao.insertBid(createBid(id = "bid_3", jobId = "job_2", professionalId = "pro_1"))

        val result = dao.getJobIdsWithBidsFlow("pro_1").first()
        assertEquals(2, result.size)
        assertTrue(result.contains("job_1"))
        assertTrue(result.contains("job_2"))
    }

    @Test
    fun `getBidsForProfessionalFlow filters by professional ID`() = runTest {
        dao.insertBid(createBid(id = "bid_1", professionalId = "pro_1"))
        dao.insertBid(createBid(id = "bid_2", professionalId = "pro_2"))
        dao.insertBid(createBid(id = "bid_3", professionalId = "pro_1"))

        val result = dao.getBidsForProfessionalFlow("pro_1").first()
        assertEquals(2, result.size)
    }

    @Test
    fun `getBidByIdSync returns correct bid`() = runTest {
        dao.insertBid(createBid(id = "bid_1"))
        dao.insertBid(createBid(id = "bid_2"))

        val result = dao.getBidByIdSync("bid_2")
        assertEquals("bid_2", result?.id)
    }

    @Test
    fun `deleteBidsForJob removes all bids for a job`() = runTest {
        dao.insertBid(createBid(id = "bid_1", jobId = "job_1"))
        dao.insertBid(createBid(id = "bid_2", jobId = "job_1"))
        dao.insertBid(createBid(id = "bid_3", jobId = "job_2"))

        dao.deleteBidsForJob("job_1")

        val job1Bids = dao.getBidsForJobFlow("job_1").first()
        val job2Bids = dao.getBidsForJobFlow("job_2").first()
        assertTrue(job1Bids.isEmpty())
        assertEquals(1, job2Bids.size)
    }

    @Test
    fun `deleteAllBids clears all bids`() = runTest {
        dao.insertBid(createBid(id = "bid_1"))
        dao.insertBid(createBid(id = "bid_2"))

        dao.deleteAllBids()

        val result = dao.getBidsForJobFlow("job_1").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `updateBid updates existing bid`() = runTest {
        dao.insertBid(createBid(id = "bid_1", amount = 50000.0, status = "PENDING"))
        dao.updateBid(createBid(id = "bid_1", amount = 50000.0, status = "ACCEPTED"))

        val result = dao.getBidByIdSync("bid_1")
        assertEquals("ACCEPTED", result?.status)
    }

    private fun createBid(
        id: String = "bid_1",
        jobId: String = "job_1",
        professionalId: String = "pro_1",
        amount: Double = 50000.0,
        status: String = "PENDING"
    ) = Bid(
        id = id, jobId = jobId, professionalId = professionalId,
        amount = amount, status = status
    )
}
