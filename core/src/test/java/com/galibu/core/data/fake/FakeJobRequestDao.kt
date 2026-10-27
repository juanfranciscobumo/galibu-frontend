package com.galibu.core.data.fake

import com.galibu.core.data.local.JobRequestDao
import com.galibu.core.data.model.JobRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeJobRequestDao : JobRequestDao {

    private val jobs = MutableStateFlow<List<JobRequest>>(emptyList())

    override fun getAllJobsFlow(): Flow<List<JobRequest>> = jobs

    override fun getJobsByCategoryFlow(category: String): Flow<List<JobRequest>> =
        jobs.map { list -> list.filter { it.category == category } }

    override fun getJobsByClientFlow(clientId: String): Flow<List<JobRequest>> =
        jobs.map { list -> list.filter { it.clientId == clientId } }

    override fun getJobsByProfessionalFlow(professionalId: String): Flow<List<JobRequest>> =
        jobs.map { list -> list.filter { it.professionalId == professionalId } }

    override fun getJobByIdFlow(id: String): Flow<JobRequest?> =
        jobs.map { list -> list.find { it.id == id } }

    override suspend fun getJobByIdSync(id: String): JobRequest? =
        jobs.value.find { it.id == id }

    override suspend fun getActiveJobWithProfessionalSync(proId: String): JobRequest? =
        jobs.value
            .filter { it.professionalId == proId && it.status != "COMPLETED" && it.status != "CANCELLED" }
            .maxByOrNull { it.createdAt }

    override fun getOpenJobsFlow(): Flow<List<JobRequest>> =
        jobs.map { list -> list.filter { it.status == "OPEN" } }

    override suspend fun insertJob(job: JobRequest) {
        jobs.value = jobs.value.filter { it.id != job.id } + job
    }

    override suspend fun updateJob(job: JobRequest) {
        jobs.value = jobs.value.map { if (it.id == job.id) job else it }
    }

    override suspend fun deleteJobById(id: String) {
        jobs.value = jobs.value.filter { it.id != id }
    }

    override suspend fun deleteAllJobs() {
        jobs.value = emptyList()
    }

    override suspend fun deleteJobsNotIn(ids: List<String>) {
        jobs.value = jobs.value.filter { it.id in ids }
    }

    fun getAllJobsSnapshot(): List<JobRequest> = jobs.value
}
