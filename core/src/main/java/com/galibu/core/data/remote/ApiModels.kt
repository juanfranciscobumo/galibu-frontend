package com.galibu.core.data.remote

import com.galibu.core.data.model.UserProfile
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String,
    val role: String,
    val department: String? = null,
    val municipality: String? = null,
    val address: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class GoogleLoginRequestDto(
    val idToken: String,
    val role: String? = null,
    /** LOGIN solo autentica; REGISTER permite crear la cuenta. */
    val mode: String = "LOGIN"
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequestDto(
    val email: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequestDto(
    val email: String,
    val code: String,
    val newPassword: String
)

@JsonClass(generateAdapter = true)
data class RefreshRequestDto(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class DeviceTokenRequestDto(
    val fcmToken: String,
    val platform: String = "ANDROID"
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    val accessToken: String = "",
    val refreshToken: String = "",
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 0L,
    val user: UserProfile? = null
)

@JsonClass(generateAdapter = true)
data class UpdateProfileDto(
    val name: String? = null,
    val phone: String? = null,
    val profilePhotoUrl: String? = null,
    val department: String? = null,
    val municipality: String? = null,
    val address: String? = null,
    val workCategory: String? = null,
    val hourlyRate: Double? = null,
    val workPhotos: String? = null,
    val workDescription: String? = null
)

@JsonClass(generateAdapter = true)
data class AddRoleDto(
    val role: String
)

@JsonClass(generateAdapter = true)
data class CreateChatMessageRequest(
    val text: String,
    val type: String? = "text"
)

@JsonClass(generateAdapter = true)
data class PasswordChangeDto(
    val currentPassword: String,
    val newPassword: String
)

@JsonClass(generateAdapter = true)
data class CreateJobDto(
    val id: String? = null,
    val category: String = "",
    val title: String,
    val description: String? = null,
    val budgetMin: Double = 0.0,
    val address: String? = null,
    val department: String? = null,
    val municipality: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val professionalId: String? = null,
    val professionalName: String? = null,
    val proHourlyRate: Double? = null,
    val suggestedPrice: Double? = null,
    val source: String? = null,
    val photos: String? = null
)

@JsonClass(generateAdapter = true)
data class AmountDto(
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class PriceProposalRequest(
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class SubmitBidDto(
    val amount: Double,
    val comment: String? = null,
    val durationHours: Int? = null,
    val quoteType: String? = "ESTIMATED",
    val visitRequired: Boolean? = null,
    val visitFee: Double? = null,
    val materialsIncluded: Boolean? = null,
    val materialsCost: Double? = null,
    val visitScheduledAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class BidEventDto(
    val id: String = "",
    val bidId: String = "",
    val jobId: String = "",
    val authorId: String = "",
    val authorRole: String = "",
    val action: String = "",
    val amount: Double = 0.0,
    val round: Int = 0,
    val note: String? = null,
    val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class RatingRequestDto(
    val stars: Int,
    val comment: String? = null
)

@JsonClass(generateAdapter = true)
data class RatingDto(
    val id: Long? = null,
    val jobId: String = "",
    val raterId: String = "",
    val ratedId: String = "",
    val stars: Int = 0,
    val comment: String? = null,
    val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class NamedDto(
    val id: Long = 0L,
    val name: String = ""
)

@JsonClass(generateAdapter = true)
data class DepartmentTreeDto(
    val name: String = "",
    val municipalities: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WalletTransactionDto(
    val id: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val jobId: String? = null,
    val description: String? = null,
    val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class BoldCheckoutResponse(
    val orderId: String = "",
    val paymentLink: String = "",
    val checkoutUrl: String = "",
    val amount: Double = 0.0,
    val status: String = "PENDING"
)

@JsonClass(generateAdapter = true)
data class BoldOrderStatusResponse(
    val orderId: String = "",
    val status: String = "PENDING",
    val amount: Double = 0.0,
    val balance: Double = 0.0,
    val transactionId: String? = null,
    val paymentMethod: String? = null,
    val concept: String? = null
)

@JsonClass(generateAdapter = true)
data class BoldOrderDto(
    val orderId: String = "",
    val amount: Double = 0.0,
    val status: String = "PENDING",
    val createdAt: Long = 0L,
    val paidAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class SubscriptionDto(
    val plan: String = "FREE",
    val status: String = "NONE",
    val expiresAt: Long? = null,
    val active: Boolean = false,
    val canWork: Boolean = false,
    val freeServicesTotal: Int = 3,
    val freeServicesUsed: Int = 0,
    val freeServicesRemaining: Int = 3,
    val monthlyPrice: Double = 29900.0,
    val yearlyPrice: Double = 299000.0
)

@JsonClass(generateAdapter = true)
data class SubscriptionCheckoutRequest(
    val plan: String
)
