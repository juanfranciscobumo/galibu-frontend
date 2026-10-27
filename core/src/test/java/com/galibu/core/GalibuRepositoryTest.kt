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
    fun `submitBid stores backend bid and refreshes job`() = runTest {
        val user = createUser(id = "pro_1", name = "Carlos Garcia")
        val negJob = createJob(id = "job_1", status = "NEGOTIATING", clientId = "client_1")
        coEvery { mockApi.jobs(any(), any(), any()) } returns listOf(negJob)
        fakeUserDao.insertUser(user)
        setCurrentUser(user)
        fakeJobDao.insertJob(createJob(id = "job_1", status = "OPEN", clientId = "client_1"))

        val apiBid = createBid(id = "bid_job_1_pro_1", jobId = "job_1", professionalId = "pro_1", status = "PENDING")
        coEvery { mockApi.submitBid(any(), any()) } returns apiBid
        coEvery { mockApi.getJob("job_1") } returns negJob
        coEvery { mockApi.jobBids("job_1") } returns listOf(apiBid)

        repository.submitBid("job_1", 75000.0, "Lo hago en 2 horas", 2)

        val bidList = fakeBidDao.getBidsForJobFlow("job_1").first()
        assertEquals(1, bidList.size)
        assertEquals("bid_job_1_pro_1", bidList[0].id)
        assertEquals("PENDING", bidList[0].status)

        val updatedJob = fakeJobDao.getJobByIdSync("job_1")
        assertNotNull(updatedJob)
        assertEquals("NEGOTIATING", updatedJob?.status)
        coVerify { mockFirebase.pushBidToCloud("job_1", apiBid) }
    }

    @Test
    fun `submitBid propagates backend failure without local writes`() = runTest {
        val user = createUser(id = "pro_1")
        setCurrentUser(user)

        coEvery { mockApi.submitBid(any(), any()) } throws RuntimeException("500")

        var failed = false
        try {
            repository.submitBid("job_1", 75000.0, "Comentario", 2)
        } catch (e: Exception) {
            failed = true
        }
        assertTrue(failed)
        assertTrue(fakeBidDao.getBidsForJobFlow("job_1").first().isEmpty())
    }

    @Test
    fun `acceptDirectHire applies server job and posts chat message`() = runTest {
        fakeJobDao.insertJob(
            createJob(
                id = "job_1",
                status = "NEGOTIATING",
                professionalId = "pro_1",
                professionalName = "Carlos Garcia",
                clientId = "client_1",
                counterOfferAmount = 80000.0
            )
        )
        val accepted = createJob(
            id = "job_1",
            status = "ACCEPTED",
            professionalId = "pro_1",
            clientId = "client_1"
        )
        coEvery { mockApi.acceptDirect("job_1") } returns accepted

        repository.acceptDirectHire("job_1")

        val updatedJob = fakeJobDao.getJobByIdSync("job_1")
        assertNotNull(updatedJob)
        assertEquals("ACCEPTED", updatedJob?.status)

        val msgList = fakeChatDao.getMessagesForJobFlow("job_1").first()
        assertEquals(1, msgList.size)
        assertEquals("system", msgList[0].senderId)
        assertTrue(msgList[0].text.contains("acept"))
    }

    @Test
    fun `rejectDirectHire applies CANCELLED from server`() = runTest {
        fakeJobDao.insertJob(
            createJob(
                id = "job_1",
                status = "NEGOTIATING",
                professionalId = "pro_1",
                professionalName = "Carlos Garcia",
                clientId = "client_1"
            )
        )
        coEvery { mockApi.rejectDirect("job_1") } returns
            createJob(id = "job_1", status = "CANCELLED", clientId = "client_1")

        repository.rejectDirectHire("job_1")

        val updatedJob = fakeJobDao.getJobByIdSync("job_1")
        assertNotNull(updatedJob)
        assertEquals("CANCELLED", updatedJob?.status)
    }

    @Test
    fun `cancelJobRequest deletes local job and bids`() = runTest {
        fakeJobDao.insertJob(createJob(id = "job_1"))
        fakeBidDao.insertBid(createBid(id = "bid_1", jobId = "job_1"))

        repository.cancelJobRequest("job_1")

        assertNull(fakeJobDao.getJobByIdSync("job_1"))
        assertNull(fakeBidDao.getBidByIdSync("bid_1"))
        coVerify { mockApi.deleteJob("job_1") }
    }

    @Test
    fun `submitCounterOfferDirectHire applies server counter and posts chat message`() = runTest {
        fakeJobDao.insertJob(
            createJob(
                id = "job_1",
                status = "NEGOTIATING",
                professionalId = "pro_1",
                clientId = "client_1"
            )
        )
        val countered = createJob(
            id = "job_1",
            status = "NEGOTIATING",
            professionalId = "pro_1",
            clientId = "client_1",
            counterOfferAmount = 90000.0,
            counterOfferSender = "CLIENT"
        )
        coEvery { mockApi.counterJob(any(), any()) } returns countered

        repository.submitCounterOfferDirectHire("job_1", 90000.0, isClient = true)

        val updatedJob = fakeJobDao.getJobByIdSync("job_1")
        assertNotNull(updatedJob)
        assertEquals("NEGOTIATING", updatedJob?.status)
        assertEquals(90000.0, updatedJob?.counterOfferAmount!!, 0.01)
        assertEquals("CLIENT", updatedJob?.counterOfferSender)

        val msgList = fakeChatDao.getMessagesForJobFlow("job_1").first()
        assertEquals(1, msgList.size)
        assertEquals("system", msgList[0].senderId)
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
