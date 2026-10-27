package com.galibu.core

import com.galibu.core.data.model.JobRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeJobRequestDaoTest {

    private val dao = com.galibu.core.data.fake.FakeJobRequestDao()

    @Test
    fun `insertJob adds job to empty list`() = runTest {
        val job = createJob(id = "job_1")
        dao.insertJob(job)

        val result = dao.getAllJobsFlow().first()
        assertEquals(1, result.size)
        assertEquals("job_1", result[0].id)
    }

    @Test
    fun `insertJob replaces existing job with same id`() = runTest {
        dao.insertJob(createJob(id = "job_1", title = "Original"))
        dao.insertJob(createJob(id = "job_1", title = "Updated"))

        val result = dao.getAllJobsFlow().first()
        assertEquals(1, result.size)
        assertEquals("Updated", result[0].title)
    }

    @Test
    fun `getJobByIdSync returns correct job`() = runTest {
        dao.insertJob(createJob(id = "job_1"))
        dao.insertJob(createJob(id = "job_2"))

        val result = dao.getJobByIdSync("job_2")
        assertEquals("job_2", result?.id)
    }

    @Test
    fun `getJobByIdSync returns null for non-existent job`() = runTest {
        val result = dao.getJobByIdSync("non_existent")
        assertEquals(null, result)
    }

    @Test
    fun `getOpenJobsFlow filters by OPEN status`() = runTest {
        dao.insertJob(createJob(id = "job_1", status = "OPEN"))
        dao.insertJob(createJob(id = "job_2", status = "NEGOTIATING"))
        dao.insertJob(createJob(id = "job_3", status = "OPEN"))

        val result = dao.getOpenJobsFlow().first()
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == "OPEN" })
    }

    @Test
    fun `deleteJobById removes correct job`() = runTest {
        dao.insertJob(createJob(id = "job_1"))
        dao.insertJob(createJob(id = "job_2"))

        dao.deleteJobById("job_1")

        val result = dao.getAllJobsFlow().first()
        assertEquals(1, result.size)
        assertEquals("job_2", result[0].id)
    }

    @Test
    fun `deleteAllJobs clears all jobs`() = runTest {
        dao.insertJob(createJob(id = "job_1"))
        dao.insertJob(createJob(id = "job_2"))
        dao.insertJob(createJob(id = "job_3"))

        dao.deleteAllJobs()

        val result = dao.getAllJobsFlow().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `deleteJobsNotIn keeps only specified jobs`() = runTest {
        dao.insertJob(createJob(id = "job_1"))
        dao.insertJob(createJob(id = "job_2"))
        dao.insertJob(createJob(id = "job_3"))

        dao.deleteJobsNotIn(listOf("job_1", "job_3"))

        val result = dao.getAllJobsFlow().first()
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "job_1" })
        assertTrue(result.any { it.id == "job_3" })
    }

    @Test
    fun `getJobsByProfessionalFlow filters correctly`() = runTest {
        dao.insertJob(createJob(id = "job_1", professionalId = "pro_1"))
        dao.insertJob(createJob(id = "job_2", professionalId = "pro_2"))
        dao.insertJob(createJob(id = "job_3", professionalId = "pro_1"))

        val result = dao.getJobsByProfessionalFlow("pro_1").first()
        assertEquals(2, result.size)
    }

    @Test
    fun `updateJob updates existing job`() = runTest {
        dao.insertJob(createJob(id = "job_1", status = "OPEN"))
        dao.updateJob(createJob(id = "job_1", status = "NEGOTIATING"))

        val result = dao.getJobByIdSync("job_1")
        assertEquals("NEGOTIATING", result?.status)
    }

    private fun createJob(
        id: String = "job_1",
        title: String = "Test Job",
        status: String = "OPEN",
        professionalId: String? = null,
        clientId: String = "client_1",
        category: String = "Plomeria"
    ) = JobRequest(
        id = id, title = title, status = status,
        professionalId = professionalId, clientId = clientId, category = category
    )
}
