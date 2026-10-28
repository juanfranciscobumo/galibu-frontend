package com.galibu.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.io.Serializable

/**
 * Campos obligatorios del perfil (los exige el completado forzoso tras Google).
 */
fun UserProfile.hasMissingRequiredFields(): Boolean {
    return name.isBlank() ||
        department.isNullOrBlank() ||
        municipality.isNullOrBlank() ||
        address.isNullOrBlank()
}

fun UserProfile.hasMissingProfessionalFields(): Boolean {
    return workCategory.isNullOrBlank() || hourlyRate == null || hourlyRate <= 0.0
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "", // "CLIENT" or "PROFESSIONAL"
    val isVerified: Boolean = true,
    val rating: Float = 5.0f,
    val completedJobs: Int = 0,
    val balance: Double = 0.0,
    val documentType: String? = null,
    val documentPhotoSimulatedUrl: String? = null,
    val workCategory: String? = null,
    val verificationStatus: String = "VERIFIED",
    val password: String = "",
    val profilePhotoUrl: String? = null,
    val department: String? = "Bogotá D.C.",
    val municipality: String? = "Bogotá",
    val address: String? = "",
    val workPhotos: String? = null,
    val workDescription: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val hourlyRate: Double? = null,
    val acceptedTerms: Boolean = false
) : Serializable

@JsonClass(generateAdapter = true)
@Entity(tableName = "job_requests")
data class JobRequest(
    @PrimaryKey val id: String = "",
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val budgetMin: Double = 0.0,
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val professionalId: String? = null,
    val professionalName: String? = null,
    val finalPrice: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completionRating: Float? = null,
    val completionReview: String? = null,
    val clientRatingOfPro: Float? = null,
    val clientReviewOfPro: String? = null,
    val proRatingOfClient: Float? = null,
    val proReviewOfClient: String? = null,
    val department: String? = "Bogotá D.C.",
    val municipality: String? = "Bogotá",
    val proHourlyRate: Double? = null,
    val suggestedPrice: Double? = null,
    val counterOfferAmount: Double? = null,
    val counterOfferSender: String? = null,
    /** Origen de la solicitud: PUBLISH (publicada) o DIRECTORY (contratación directa). */
    val source: String? = null,
  /** Fotos adjuntas a la solicitud (pipe-separated CSV de URLs/data URIs). */
    val photos: String? = null
) : Serializable

/**
 * Direct-chat threads created only to open a conversation (ids "chat_*") are not
 * real job requests and must be hidden from solicitudes lists/counters.
 */
fun JobRequest.isDirectChat(): Boolean = id.startsWith("chat_")

@JsonClass(generateAdapter = true)
@Entity(tableName = "bids")
data class Bid(
    @PrimaryKey val id: String = "",
    val jobId: String = "",
    val professionalId: String = "",
    val professionalName: String = "",
    val professionalRating: Float = 5.0f,
    val professionalPhoto: String = "",
    val amount: Double = 0.0,
    val comment: String = "",
    val durationHours: Int = 1,
    val status: String = "",
    val lastCounterOfferAmount: Double? = null,
    val suggestedPrice: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val jobId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "text",
    val read: Boolean = false
) : Serializable

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    /** Solicitud relacionada (para abrir su detalle al tocar). Null en notificaciones viejas. */
    val jobId: String? = null
) : Serializable

data class FirebaseDepartment(
    val name: String = "",
    val municipalities: List<String> = emptyList()
) : Serializable

data class FirebaseCategory(
    val name: String = ""
) : Serializable

