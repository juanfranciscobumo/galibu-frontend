package com.galibu.core

import com.galibu.core.data.model.JobRequest
import com.galibu.core.data.repository.GalibuRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation

class SyncJobsSnapshotTest : BaseRepositoryTest() {

    private suspend fun callSyncJobsSnapshot(
        jobs: List<JobRequest>,
        isFromCache: Boolean,
        currentUserId: String
    ) {
        val method = GalibuRepository::class.java.getDeclaredMethod(
            "syncJobsSnapshot",
            List::class.java,
            Boolean::class.java,
            String::class.java,
            Continuation::class.java
        )
        method.isAccessible = true
        method.invoke(repository, jobs, isFromCache, currentUserId, null)
    }

    @Test
    fun `syncJobsSnapshot inserts jobs into local DB`() = runTest {
        val jobs = listOf(
            createJob(id = "job_1", status = "OPEN"),
            createJob(id = "job_2", status = "NEGOTIATING")
        )

        callSyncJobsSnapshot(jobs, false, "user_1")

        val job1 = fakeJobDao.getJobByIdSync("job_1")
        val job2 = fakeJobDao.getJobByIdSync("job_2")
        assertEquals("OPEN", job1?.status)
        assertEquals("NEGOTIATING", job2?.status)
    }

    @Test
    fun `syncJobsSnapshot deletes jobs not in server list when not from cache`() = runTest {
        fakeJobDao.insertJob(createJob(id = "local_only_job", status = "OPEN"))
        fakeJobDao.insertJob(createJob(id = "server_job", status = "OPEN"))

        val serverJobs = listOf(createJob(id = "server_job", status = "OPEN"))
        callSyncJobsSnapshot(serverJobs, false, "user_1")

        val deletedJob = fakeJobDao.getJobByIdSync("local_only_job")
        assertEquals(null, deletedJob)

        val keptJob = fakeJobDao.getJobByIdSync("server_job")
        assertEquals("OPEN", keptJob?.status)
    }

    @Test
    fun `syncJobsSnapshot preserves locally modified jobs during delete`() = runTest {
        fakeJobDao.insertJob(createJob(id = "local_job", status = "NEGOTIATING"))

        val locallyModifiedField = GalibuRepository::class.java.getDeclaredField("locallyModifiedJobIds")
        locallyModifiedField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val locallyModified = locallyModifiedField.get(repository) as MutableSet<String>
        locallyModified.add("local_job")

        val serverJobs = listOf(createJob(id = "server_job", status = "OPEN"))
        callSyncJobsSnapshot(serverJobs, false, "user_1")

        val preservedJob = fakeJobDao.getJobByIdSync("local_job")
        assertEquals("NEGOTIATING", preservedJob?.status)
    }

    @Test
    fun `syncJobsSnapshot deletes all jobs when server returns empty and no local modifications`() = runTest {
        fakeJobDao.insertJob(createJob(id = "job_1", status = "OPEN"))

        callSyncJobsSnapshot(emptyList(), false, "user_1")

        val allJobs = fakeJobDao.getAllJobsSnapshot()
        assertTrue(allJobs.isEmpty())
    }

    @Test
    fun `syncJobsSnapshot does not delete when isFromCache is true`() = runTest {
        fakeJobDao.insertJob(createJob(id = "job_1", status = "OPEN"))
        fakeJobDao.insertJob(createJob(id = "job_2", status = "NEGOTIATING"))

        val serverJobs = listOf(createJob(id = "job_1", status = "OPEN"))
        callSyncJobsSnapshot(serverJobs, true, "user_1")

        val job2 = fakeJobDao.getJobByIdSync("job_2")
        assertEquals("NEGOTIATING", job2?.status)
    }

    @Test
    fun `syncJobsSnapshot creates notification for new direct hire job`() = runTest {
        val jobs = listOf(
            createJob(
                id = "job_1",
                status = "NEGOTIATING",
                professionalId = "pro_1",
                clientName = "Juan Perez",
                category = "Plomeria"
            )
        )

        callSyncJobsSnapshot(jobs, false, "pro_1")

        val notifList = fakeNotificationDao.getNotificationsForUserFlow("pro_1").first()
        assertTrue(notifList.any { it.title.contains("contrataci") })
    }
}
