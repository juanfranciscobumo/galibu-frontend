package com.galibu.core.data.firebase

import android.util.Log
import androidx.core.graphics.scale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.gms.tasks.Task

// A simple, pure await extension for Firebase Tasks to avoid extra library dependencies
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { t ->
        if (t.isSuccessful) {
            cont.resume(t.result)
        } else {
            cont.resumeWithException(t.exception ?: RuntimeException("Error en operación de Firebase Task"))
        }
    }
}

/**
 * Servicios de Firebase que siguen en uso:
 *  - Storage: fotos de perfil, trabajos e imágenes del chat.
 *  - Auth: recuperación de contraseña ("¿Olvidaste tu contraseña?") y cierre de sesión.
 *  - Firestore: indicador "está escribiendo..." del chat.
 *
 * El resto de la aplicación (perfiles, solicitudes, ofertas, chat y notificaciones)
 * usa el backend propio.
 */
class FirebaseService {

    // Lazy reference to FirebaseAuth instance to prevent early-crash if SDK is not initialized
    val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Instancia Firebase no inicializada (falta google-services.json o inicializar FirebaseApp). Se usará simulador local.", e)
            null
        }
    }

    fun isFirebaseInitialized(): Boolean {
        return auth != null
    }

    /**
     * Firebase Auth Sign Out
     */
    fun firebaseSignOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error al cerrar sesión de Firebase Auth", e)
        }
    }

    /**
     * Firebase Auth Send Password Reset Email
     */
    suspend fun firebaseSendPasswordResetEmail(email: String) {
        val currentAuth = auth ?: throw IllegalStateException("Firebase Auth no disponible")
        try {
            currentAuth.sendPasswordResetEmail(email).awaitTask()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error enviando correo de recuperación en Firebase Auth: ${e.localizedMessage}")
            throw e
        }
    }

    /**
     * Upload user's work/portfolio photo.
     * Simulated as uploading files to Firebase Storage: `gs://ofreceya-jobs/work_photos/{userId}_{photoId}.jpg`
     */
    suspend fun uploadWorkPhoto(userId: String, photoId: String, photoBytes: ByteArray): String {
        Log.d("FirebaseService", "Uploading work photo for user: $userId, photoId: $photoId to Firebase Storage.")

        if (isFirebaseInitialized()) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                val photoRef = storageRef.child("work_photos/${userId}_${photoId}.jpg")
                photoRef.putBytes(photoBytes).awaitTask()
                val downloadUrl = photoRef.downloadUrl.awaitTask()
                return downloadUrl.toString()
            } catch (e: Exception) {
                Log.e("FirebaseService", "Error uploading work photo to Firebase Storage: ${e.localizedMessage}. Usando fallback de simulación.")
            }
        }

        // Fallback: compress and encode the actual selected image to Base64 to ensure it perseveres in Firestore
        try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
            if (bitmap != null) {
                val maxDim = 300 // slightly larger for work photos
                val width = bitmap.width
                val height = bitmap.height
                val (newWidth, newHeight) = if (width > height) {
                    val ratio = height.toFloat() / width
                    (maxDim to (maxDim * ratio).toInt())
                } else {
                    val ratio = width.toFloat() / height
                    (((maxDim * ratio).toInt()) to maxDim)
                }
                val scaledBitmap = bitmap.scale(newWidth, newHeight)
                val out = java.io.ByteArrayOutputStream()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
                val compressedBytes = out.toByteArray()
                val base64Str = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                return "data:image/jpeg;base64,$base64Str"
            }
        } catch (ex: Exception) {
            Log.e("FirebaseService", "Failed to compress work photo to Base64: ${ex.localizedMessage}")
        }

        return "https://images.unsplash.com/photo-1581094794329-c8112a89af12?w=300&h=300&fit=crop"
    }

    /**
     * Upload chat image.
     */
    suspend fun uploadChatImage(jobId: String, imageId: String, photoBytes: ByteArray): String {
        Log.d("FirebaseService", "Uploading chat image for job: $jobId to Firebase Storage.")
        if (isFirebaseInitialized()) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                val photoRef = storageRef.child("chat_images/${jobId}_${imageId}.jpg")
                photoRef.putBytes(photoBytes).awaitTask()
                val downloadUrl = photoRef.downloadUrl.awaitTask()
                return downloadUrl.toString()
            } catch (e: Exception) {
                Log.e("FirebaseService", "Error uploading chat image to Firebase Storage: ${e.localizedMessage}")
            }
        }

        try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
            if (bitmap != null) {
                val maxDim = 400
                val width = bitmap.width
                val height = bitmap.height
                val (newWidth, newHeight) = if (width > height) {
                    val ratio = height.toFloat() / width
                    Pair(maxDim, (maxDim * ratio).toInt())
                } else {
                    val ratio = width.toFloat() / height
                    Pair((maxDim * ratio).toInt(), maxDim)
                }
                val scaledBitmap = bitmap.scale(newWidth, newHeight)
                val out = java.io.ByteArrayOutputStream()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
                val compressedBytes = out.toByteArray()
                val base64Str = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                return "data:image/jpeg;base64,$base64Str"
            }
        } catch (ex: Exception) {}

        return "https://images.unsplash.com/photo-1581094794329-c8112a89af12?w=300&h=300&fit=crop"
    }

    /**
     * Upload user's avatar profile photo.
     * Simulated as uploading files to Firebase Storage: `gs://ofreceya-jobs/profiles/{userId}_avatar.jpg`
     */
    suspend fun uploadProfilePhoto(userId: String, photoBytes: ByteArray): String {
        Log.d("FirebaseService", "Uploading profile photo for user: $userId to Firebase Storage.")

        if (isFirebaseInitialized()) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                val photoRef = storageRef.child("profiles/${userId}_avatar.jpg")
                photoRef.putBytes(photoBytes).awaitTask()
                val downloadUrl = photoRef.downloadUrl.awaitTask()
                return downloadUrl.toString()
            } catch (e: Exception) {
                Log.e("FirebaseService", "Error uploading profile photo to Firebase Storage: ${e.localizedMessage}. Usando fallback de simulación.")
            }
        }

        // Fallback: compress and encode the actual selected image to Base64 to ensure it perseveres in Firestore
        try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
            if (bitmap != null) {
                val maxDim = 150
                val width = bitmap.width
                val height = bitmap.height
                val (newWidth, newHeight) = if (width > height) {
                    val ratio = height.toFloat() / width
                    (maxDim to (maxDim * ratio).toInt())
                } else {
                    val ratio = width.toFloat() / height
                    (((maxDim * ratio).toInt()) to maxDim)
                }
                val scaledBitmap = bitmap.scale(newWidth, newHeight)
                val out = java.io.ByteArrayOutputStream()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
                val compressedBytes = out.toByteArray()
                val base64Str = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                return "data:image/jpeg;base64,$base64Str"
            }
        } catch (ex: Exception) {
            Log.e("FirebaseService", "Failed to compress profile photo to Base64: ${ex.localizedMessage}")
        }

        // Simulation/Fallback return path inside real environment to prevent crash (preset high-quality avatar)
        return "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&h=150&fit=crop"
    }

    /**
     * Set live user typing status for a job chat in Firestore path: `/typing/{jobId}`
     */
    suspend fun setTypingStatusInCloud(jobId: String, userId: String, typing: Boolean) {
        if (!isFirebaseInitialized()) return
        try {
            val db = FirebaseFirestore.getInstance()
            val data = hashMapOf<String, Any>(
                userId to typing
            )
            db.collection("typing")
                .document(jobId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .awaitTask()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error setting typing status in cloud: ${e.localizedMessage}")
        }
    }

    /**
     * Subscribe in real-time to user typing indicators for a job chat in Firestore path: `/typing/{jobId}`
     */
    fun listenToTypingStatus(jobId: String, onTypingChanged: (Map<String, Boolean>) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        if (!isFirebaseInitialized()) return null
        return try {
            val db = FirebaseFirestore.getInstance()
            db.collection("typing")
                .document(jobId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseService", "Listen to typing status failed", error)
                        return@addSnapshotListener
                    }
                    val typingMap = mutableMapOf<String, Boolean>()
                    if (snapshot != null && snapshot.exists()) {
                        val data = snapshot.data
                        if (data != null) {
                            for ((key, value) in data) {
                                if (value is Boolean) {
                                    typingMap[key] = value
                                }
                            }
                        }
                    }
                    onTypingChanged(typingMap)
                }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error listening to typing status: ${e.localizedMessage}")
            null
        }
    }
}
