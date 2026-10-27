package com.galibu.core.data.fake

import com.galibu.core.data.local.BidDao
import com.galibu.core.data.model.Bid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBidDao : BidDao {

    private val bids = MutableStateFlow<List<Bid>>(emptyList())

    override fun getBidsForJobFlow(jobId: String): Flow<List<Bid>> =
        bids.map { list -> list.filter { it.jobId == jobId } }

    override fun getJobIdsWithBidsFlow(professionalId: String): Flow<List<String>> =
        bids.map { list ->
            list.filter { it.professionalId == professionalId }
                .map { it.jobId }
                .distinct()
        }

    override fun getBidsForProfessionalFlow(professionalId: String): Flow<List<Bid>> =
        bids.map { list -> list.filter { it.professionalId == professionalId } }

    override suspend fun getBidByIdSync(id: String): Bid? =
        bids.value.find { it.id == id }

    override suspend fun getBidsForJobAndProfessionalSync(jobId: String, professionalId: String): List<Bid> =
        bids.value.filter { it.jobId == jobId && it.professionalId == professionalId }

    override suspend fun getBidsForJobSync(jobId: String): List<Bid> =
        bids.value.filter { it.jobId == jobId }

    override suspend fun deleteBidById(bidId: String) {
        bids.value = bids.value.filter { it.id != bidId }
    }

    override suspend fun insertBid(bid: Bid) {
        bids.value = bids.value.filter { it.id != bid.id } + bid
    }

    override suspend fun updateBid(bid: Bid) {
        bids.value = bids.value.map { if (it.id == bid.id) bid else it }
    }

    override suspend fun deleteBidsForJob(jobId: String) {
        bids.value = bids.value.filter { it.jobId != jobId }
    }

    override suspend fun deleteAllBids() {
        bids.value = emptyList()
    }

    fun getBidsSnapshot(): List<Bid> = bids.value
}
