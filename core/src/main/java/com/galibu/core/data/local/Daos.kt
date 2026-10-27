package com.galibu.core.data.local

import androidx.room.*
import com.galibu.core.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id")
    fun getUserByIdFlow(id: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getUserByIdSync(id: String): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE email = :email LIMIT 1")
    suspend fun getUserByEmailSync(email: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile)

    @Update
    suspend fun updateUser(user: UserProfile)

    @Query("DELETE FROM user_profiles WHERE id = :id")
    suspend fun deleteUserById(id: String)

    @Query("DELETE FROM user_profiles WHERE (role = 'PROFESSIONAL' OR role = 'BOTH') AND id NOT IN (:ids) AND id != :currentUserId")
    suspend fun deleteProfessionalsNotIn(ids: List<String>, currentUserId: String)

    @Query("DELETE FROM user_profiles WHERE (role = 'PROFESSIONAL' OR role = 'BOTH') AND id != :currentUserId")
    suspend fun deleteAllProfessionalsExcept(currentUserId: String)

    @Query("SELECT * FROM user_profiles WHERE (role = 'PROFESSIONAL' OR role = 'BOTH') ORDER BY name ASC")
    fun getProfessionalsFlow(): Flow<List<UserProfile>>

    @Query("DELETE FROM user_profiles")
    suspend fun deleteAllUserProfiles()
}

@Dao
interface JobRequestDao {
    @Query("SELECT * FROM job_requests ORDER BY createdAt DESC")
    fun getAllJobsFlow(): Flow<List<JobRequest>>

    @Query("SELECT * FROM job_requests WHERE category = :category ORDER BY createdAt DESC")
    fun getJobsByCategoryFlow(category: String): Flow<List<JobRequest>>

    @Query("SELECT * FROM job_requests WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getJobsByClientFlow(clientId: String): Flow<List<JobRequest>>

    @Query("SELECT * FROM job_requests WHERE professionalId = :professionalId ORDER BY createdAt DESC")
    fun getJobsByProfessionalFlow(professionalId: String): Flow<List<JobRequest>>

    @Query("SELECT * FROM job_requests WHERE id = :id")
    fun getJobByIdFlow(id: String): Flow<JobRequest?>

    @Query("SELECT * FROM job_requests WHERE id = :id")
    suspend fun getJobByIdSync(id: String): JobRequest?

    @Query("SELECT * FROM job_requests WHERE professionalId = :proId AND status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveJobWithProfessionalSync(proId: String): JobRequest?

    @Query("SELECT * FROM job_requests WHERE status = 'OPEN' ORDER BY createdAt DESC")
    fun getOpenJobsFlow(): Flow<List<JobRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobRequest)

    @Update
    suspend fun updateJob(job: JobRequest)

    @Query("DELETE FROM job_requests WHERE id = :id")
    suspend fun deleteJobById(id: String)

    @Query("DELETE FROM job_requests")
    suspend fun deleteAllJobs()

    @Query("DELETE FROM job_requests WHERE id NOT IN (:ids)")
    suspend fun deleteJobsNotIn(ids: List<String>)
}

@Dao
interface BidDao {
    @Query("SELECT * FROM bids WHERE jobId = :jobId ORDER BY createdAt DESC")
    fun getBidsForJobFlow(jobId: String): Flow<List<Bid>>

    @Query("SELECT DISTINCT jobId FROM bids WHERE professionalId = :professionalId")
    fun getJobIdsWithBidsFlow(professionalId: String): Flow<List<String>>

    @Query("SELECT * FROM bids WHERE professionalId = :professionalId")
    fun getBidsForProfessionalFlow(professionalId: String): Flow<List<Bid>>

    @Query("SELECT * FROM bids WHERE id = :id")
    suspend fun getBidByIdSync(id: String): Bid?

    @Query("SELECT * FROM bids WHERE jobId = :jobId")
    suspend fun getBidsForJobSync(jobId: String): List<Bid>

    @Query("SELECT * FROM bids WHERE jobId = :jobId AND professionalId = :professionalId")
    suspend fun getBidsForJobAndProfessionalSync(jobId: String, professionalId: String): List<Bid>

    @Query("DELETE FROM bids WHERE id = :bidId")
    suspend fun deleteBidById(bidId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBid(bid: Bid)

    @Update
    suspend fun updateBid(bid: Bid)

    @Query("DELETE FROM bids WHERE jobId = :jobId")
    suspend fun deleteBidsForJob(jobId: String)

    @Query("DELETE FROM bids")
    suspend fun deleteAllBids()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE jobId = :jobId ORDER BY timestamp ASC")
    fun getMessagesForJobFlow(jobId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("UPDATE chat_messages SET read = 1 WHERE jobId = :jobId AND senderId != :currentUserId")
    suspend fun markMessagesAsRead(jobId: String, currentUserId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllChatMessages()
}

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUserFlow(userId: String): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE userId = :userId AND jobId = :jobId")
    suspend fun markJobAsRead(userId: String, jobId: String)

    @Query("DELETE FROM app_notifications")
    suspend fun deleteAllNotifications()
}

