package com.galibu.core.data.remote

import com.galibu.core.data.model.Bid
import com.galibu.core.data.model.ChatMessage
import com.galibu.core.data.model.JobRequest
import com.galibu.core.data.model.UserProfile
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Auth (public; no token needed)
    @POST("auth/register")
    suspend fun register(@Body req: RegisterRequestDto): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body req: LoginRequestDto): AuthResponseDto

    @POST("auth/google")
    suspend fun google(@Body req: GoogleLoginRequestDto): AuthResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Body req: RefreshRequestDto): AuthResponseDto

    // Users
    @GET("users/me")
    suspend fun getMe(): UserProfile

    @PATCH("users/me")
    suspend fun updateMe(@Body req: UpdateProfileDto): UserProfile

    @POST("users/me/roles")
    suspend fun addRole(@Body req: AddRoleDto): UserProfile

    @PATCH("users/me/password")
    suspend fun changePassword(@Body req: PasswordChangeDto)

    @DELETE("users/me")
    suspend fun deleteMe()

    @GET("users/professionals")
    suspend fun professionals(): List<UserProfile>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserProfile

    // Jobs
    @GET("jobs/open")
    suspend fun openJobs(): List<JobRequest>

    @GET("jobs")
    suspend fun jobs(
        @Query("clientId") clientId: String?,
        @Query("professionalId") professionalId: String?,
        @Query("status") status: String?
    ): List<JobRequest>

    @GET("jobs/{id}")
    suspend fun getJob(@Path("id") id: String): JobRequest

    @POST("jobs")
    suspend fun createJob(@Body req: CreateJobDto): JobRequest

    @PATCH("jobs/{id}/cancel")
    suspend fun cancelJob(@Path("id") id: String): JobRequest

    @DELETE("jobs/{id}")
    suspend fun deleteJob(@Path("id") id: String)

    @POST("jobs/{id}/counter")
    suspend fun counterJob(@Path("id") id: String, @Body req: AmountDto): JobRequest

    @POST("jobs/{id}/accept-direct")
    suspend fun acceptDirect(@Path("id") id: String): JobRequest

    @POST("jobs/{id}/reject-direct")
    suspend fun rejectDirect(@Path("id") id: String): JobRequest

    @POST("jobs/{id}/start")
    suspend fun startService(@Path("id") id: String): JobRequest

    @POST("jobs/{id}/finish")
    suspend fun finishService(@Path("id") id: String, @Body req: RatingRequestDto): JobRequest

    // Bids
    @GET("jobs/{jobId}/bids")
    suspend fun jobBids(@Path("jobId") jobId: String): List<Bid>

    @GET("bids/mine")
    suspend fun myBids(): List<Bid>

    @POST("jobs/{jobId}/bids")
    suspend fun submitBid(@Path("jobId") jobId: String, @Body req: SubmitBidDto): Bid

    @POST("bids/{bidId}/counter")
    suspend fun counterBid(@Path("bidId") bidId: String, @Body req: AmountDto): Bid

    @POST("bids/{bidId}/reject")
    suspend fun rejectBid(@Path("bidId") bidId: String): Bid

    @POST("bids/{bidId}/accept")
    suspend fun acceptBid(@Path("bidId") bidId: String): Bid

    // Wallet
    @POST("wallet/recharge")
    suspend fun rechargeWallet(@Body req: AmountDto): WalletTransactionDto

    @GET("wallet/transactions")
    suspend fun walletTransactions(): List<WalletTransactionDto>

    // Wallet · pagos en línea con Bold
    @POST("wallet/bold/checkout")
    suspend fun createBoldCheckout(@Body req: AmountDto): BoldCheckoutResponse

    @GET("wallet/bold/orders/{orderId}")
    suspend fun boldOrderStatus(@Path("orderId") orderId: String): BoldOrderStatusResponse

    @GET("wallet/bold/orders")
    suspend fun boldOrders(): List<BoldOrderDto>

    // Ratings
    @POST("jobs/{jobId}/ratings")
    suspend fun rateJob(@Path("jobId") jobId: String, @Body req: RatingRequestDto): RatingDto

    @GET("jobs/{jobId}/ratings")
    suspend fun jobRatings(@Path("jobId") jobId: String): List<RatingDto>

    @GET("users/{userId}/ratings")
    suspend fun userRatings(@Path("userId") userId: String): List<RatingDto>

    // Chat
    @GET("jobs/{jobId}/chats")
    suspend fun jobChats(@Path("jobId") jobId: String): List<ChatMessage>

    @POST("jobs/{jobId}/chats")
    suspend fun sendChat(@Path("jobId") jobId: String, @Body req: CreateChatMessageRequest): ChatMessage

    @POST("jobs/{jobId}/chats/read")
    suspend fun markChatsRead(@Path("jobId") jobId: String)

    @DELETE("jobs/{jobId}/chats/{messageId}")
    suspend fun deleteChat(@Path("jobId") jobId: String, @Path("messageId") messageId: String)

    // Geo (public)
    @GET("geo/departments")
    suspend fun departments(): List<NamedDto>

    @GET("geo/departments/{id}/municipalities")
    suspend fun municipalities(@Path("id") id: Long): List<NamedDto>

    @GET("geo/tree")
    suspend fun geoTree(): List<DepartmentTreeDto>

    @GET("geo/categories")
    suspend fun categories(): List<NamedDto>

    @POST("geo/categories")
    suspend fun addCategory(@Body req: Map<String, String>): NamedDto
}

/** Synchronous variant used by the token authenticator (own bare client, no auth loop). */
interface AuthRefreshApi {
    @POST("auth/refresh")
    fun refresh(@Body req: RefreshRequestDto): Call<AuthResponseDto>
}
