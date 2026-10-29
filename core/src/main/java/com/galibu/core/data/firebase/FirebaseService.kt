package com.galibu.core.data.firebase

import android.util.Log
import androidx.core.graphics.scale
import com.galibu.core.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
 * FirebaseService: Manages cloud database synchronization and file storage for identity documents.
 * 
 * DESIGN PROTOCOL:
 * Supports real-time state synchronizations. It includes both high-fidelity simulation and references
 * to real Firebase FireStore & Firebase Storage SDK implementations.
 */
class FirebaseService {
    private class CompositeListenerRegistration(
        private val registrations: List<ListenerRegistration>
    ) : ListenerRegistration {
        override fun remove() {
            registrations.forEach { it.remove() }
        }
    }

    private val _isCloudSyncEnabled = MutableStateFlow(true)
    val isCloudSyncEnabled: Flow<Boolean> = _isCloudSyncEnabled.asStateFlow()

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
     * Firebase Auth Register
     */
    suspend fun firebaseRegister(email: String, password: String): String? {
        val currentAuth = auth ?: throw IllegalStateException("Firebase Auth no disponible")
        return try {
            val result = currentAuth.createUserWithEmailAndPassword(email, password).awaitTask()
            result.user?.uid
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error durante el registro de Firebase Auth: ${e.localizedMessage}")
            throw e
        }
    }

    /**
     * Firebase Auth Login
     */
    suspend fun firebaseLogin(email: String, password: String): String? {
        val currentAuth = auth ?: throw IllegalStateException("Firebase Auth no disponible")
        return try {
            val result = currentAuth.signInWithEmailAndPassword(email, password).awaitTask()
            result.user?.uid
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error durante el inicio de sesión de Firebase Auth: ${e.localizedMessage}")
            throw e
        }
    }

    /**
     * Authenticate with Google credential
     */
    suspend fun authenticateGoogleCredential(idToken: String): String? {
        val currentAuth = auth ?: throw java.lang.IllegalStateException("Firebase Auth no disponible")
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = currentAuth.signInWithCredential(credential).awaitTask()
            result.user?.uid
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error durante el inicio de sesión con Google en Firebase Auth: ${e.localizedMessage}")
            throw e
        }
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
     * UID of the currently signed-in Firebase Auth user, or null when there is no
     * real session. Used to restore the session after the app process is killed and
     * to refuse "ghost logins" (UI user without an Auth session, where every
     * Firestore read would be denied by Security Rules).
     */
    fun getCurrentAuthUid(): String? {
        return try {
            auth?.currentUser?.uid
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error obteniendo UID de Firebase Auth", e)
            null
        }
    }

    /**
     * Observes Firebase Auth ID-token changes: sign-in, sign-out AND token refresh.
     * Returns an opaque handle to pass to [removeAuthStateListener], or null when
     * Auth is unavailable.
     */
    fun addIdTokenListener(onChange: (String?) -> Unit): Any? {
        return try {
            val currentAuth = auth ?: return null
            val listener = FirebaseAuth.IdTokenListener { firebaseAuth ->
                onChange(firebaseAuth.currentUser?.uid)
            }
            currentAuth.addIdTokenListener(listener)
            listener
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error registrando listener de Firebase Auth", e)
            null
        }
    }

    fun removeAuthStateListener(handle: Any?) {
        try {
            (handle as? FirebaseAuth.IdTokenListener)?.let { auth?.removeIdTokenListener(it) }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error removiendo listener de Firebase Auth", e)
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

    fun toggleCloudSync(enabled: Boolean) {
        _isCloudSyncEnabled.value = enabled
    }

    /**
     * Upload professional's verification document.
     * Simulated as uploading files to Firebase Storage: `gs://ofreceya-jobs/documents/{userId}_id.jpg`
     */
    suspend fun uploadIdentityDocument(userId: String, documentBytes: ByteArray): String {
        Log.d("FirebaseService", "Uploading identity document for user: $userId to Firebase Storage.")
        
        if (isFirebaseInitialized()) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                val documentRef = storageRef.child("documents/${userId}_id.jpg")
                documentRef.putBytes(documentBytes).awaitTask()
                val downloadUrl = documentRef.downloadUrl.awaitTask()
                return downloadUrl.toString()
            } catch (e: Exception) {
                Log.e("FirebaseService", "Error uploading identity document to Firebase Storage: ${e.localizedMessage}. Usando fallback de simulación.")
            }
        }
        
        // Simulation/Fallback return path inside real environment to prevent crash
        return "https://firebasestorage.googleapis.com/v0/b/ofreceya-jobs.appspot.com/o/documents%2F${userId}_id.jpg?alt=media&token=simulated-token"
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
     * Sync user profile to Firestore database: `/users/{userId}`
     */
    suspend fun syncUserProfileToCloud(profile: UserProfile) {
        Log.d("FirebaseService", "Syncing profile to Firestore collection '/users/${profile.id}'")
        
        if (isFirebaseInitialized()) {
            try {
                val firebaseFirestore = FirebaseFirestore.getInstance()
                firebaseFirestore.collection("users")
                    .document(profile.id)
                    .set(profile)
                    .awaitTask()
                Log.d("FirebaseService", "Profile synced successfully to Firestore")
            } catch (e: Exception) {
                Log.e("FirebaseService", "Failed to sync profile directory with Firestore: ${e.localizedMessage}")
                throw e
            }
        }
    }

    /**
     * Delete user profile from Firestore: `/users/{userId}`
     */
    suspend fun deleteUserProfileFromCloud(userId: String) {
        Log.d("FirebaseService", "Deleting user profile $userId from cloud Firestore")
        if (isFirebaseInitialized()) {
            try {
                val firebaseFirestore = FirebaseFirestore.getInstance()
                firebaseFirestore.collection("users")
                    .document(userId)
                    .delete()
                    .awaitTask()
                Log.d("FirebaseService", "User profile deleted from Cloud Firestore successfully")
            } catch (e: Exception) {
                Log.e("FirebaseService", "Failed to delete user profile from Firestore: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Delete signed-in Google/Email user from Firebase Auth
     */
    suspend fun deleteCurrentUserAuth() {
        val currentAuth = auth ?: return
        try {
            currentAuth.currentUser?.delete()?.awaitTask()
            Log.d("FirebaseService", "Firebase Auth current user deleted successfully")
        } catch (e: Exception) {
            Log.e("FirebaseService", "Failed to delete user from Firebase Auth: ${e.localizedMessage}")
        }
    }

    /**
     * Match and push job posts to Firestore database: `/jobs/{jobId}`
     */
    suspend fun pushJobToCloud(job: JobRequest) {
        Log.d("FirebaseService", "Pushing job request info to Firestore: '/jobs/${job.id}'")
        
        if (isFirebaseInitialized()) {
            try {
                val firebaseFirestore = FirebaseFirestore.getInstance()
                firebaseFirestore.collection("jobs")
                    .document(job.id)
                    .set(job)
                    .awaitTask()
                Log.d("FirebaseService", "Job post synced successfully to Firestore")
            } catch (e: Exception) {
                Log.e("FirebaseService", "Failed to sync job request: ${e.localizedMessage}")
                throw e
            }
        }
    }

    /**
     * Fetch a job post directly from Firestore by ID
     */
    suspend fun getJobFromCloud(jobId: String): JobRequest? {
        if (!isFirebaseInitialized()) return null
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("jobs").document(jobId).get().awaitTask()
            doc.toObject(JobRequest::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error getting job from cloud: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Delete job post from Firestore database: `/jobs/{jobId}`
     */
    suspend fun deleteJobFromCloud(jobId: String) {
        Log.d("FirebaseService", "Deleting job request from Firestore: '/jobs/$jobId'")
        if (isFirebaseInitialized()) {
            try {
                val firebaseFirestore = FirebaseFirestore.getInstance()
                firebaseFirestore.collection("jobs")
                    .document(jobId)
                    .delete()
                    .awaitTask()
                Log.d("FirebaseService", "Job post deleted successfully from Firestore")
            } catch (e: Exception) {
                Log.e("FirebaseService", "Failed to delete job request from Firestore: ${e.localizedMessage}")
                throw e
            }
        }
    }

    /**
     * Delete a single bid from Firestore: `/jobs/{jobId}/bids/{bidId}`
     */
    suspend fun deleteBidFromCloud(jobId: String, bidId: String) {
        Log.d("FirebaseService", "Deleting bid from Firestore: '/jobs/$jobId/bids/$bidId'")
        if (isFirebaseInitialized()) {
            try {
                val firebaseFirestore = FirebaseFirestore.getInstance()
                firebaseFirestore.collection("jobs")
                    .document(jobId)
                    .collection("bids")
                    .document(bidId)
                    .delete()
                    .awaitTask()
                Log.d("FirebaseService", "Bid deleted successfully from Firestore")
            } catch (e: Exception) {
                Log.e("FirebaseService", "Failed to delete bid from Firestore: ${e.localizedMessage}")
                throw e
            }
        }
    }

    /**
     * Synchronize a proposal bid in Cloud Firestore under subcollection: `/jobs/{jobId}/bids/{bidId}`
     */
    suspend fun pushBidToCloud(jobId: String, bid: Bid) {
        Log.d("FirebaseService", "Synchronizing competitive bid: '/jobs/$jobId/bids/${bid.id}'")
        
        if (isFirebaseInitialized()) {
            try {
                val firebaseFirestore = FirebaseFirestore.getInstance()
                firebaseFirestore.collection("jobs")
                    .document(jobId)
                    .collection("bids")
                    .document(bid.id)
                    .set(bid)
                    .awaitTask()
                Log.d("FirebaseService", "Bid offer synced successfully to Firestore")
            } catch (e: Exception) {
                Log.e("FirebaseService", "Failed to sync bid offer with Firestore: ${e.localizedMessage}")
                throw e
            }
        }
    }

    /**
     * Post live chat message to Firestore path: `/jobs/{jobId}/chats/{msgId}`
     */
    suspend fun sendChatMessageToCloud(message: ChatMessage) {
        Log.d("FirebaseService", "Posting real-time secure message: '/jobs/${message.jobId}/chats'")
        
        if (isFirebaseInitialized()) {
            try {
                val firebaseFirestore = FirebaseFirestore.getInstance()
                firebaseFirestore.collection("jobs")
                    .document(message.jobId)
                    .collection("chats")
                    .document(message.id.toString())
                    .set(message)
                    .awaitTask()
                Log.d("FirebaseService", "Chat message synced successfully to Firestore")
            } catch (e: Exception) {
                Log.e("FirebaseService", "Failed to upload chat message with Firestore: ${e.localizedMessage}")
                throw e
            }
        }
    }

    /**
     * Mark all secure chat messages as read in Firestore for a given jobId
     */
    suspend fun markChatMessagesAsReadInCloud(jobId: String, currentUserId: String) {
        if (!isFirebaseInitialized()) return
        try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("jobs")
                .document(jobId)
                .collection("chats")
                .whereEqualTo("read", false)
                .get()
                .awaitTask()
            for (doc in snapshot.documents) {
                val senderId = doc.getString("senderId") ?: ""
                if (senderId != currentUserId) {
                    db.collection("jobs")
                        .document(jobId)
                        .collection("chats")
                        .document(doc.id)
                        .update("read", true)
                        .awaitTask()
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error marking messages as read in cloud: ${e.localizedMessage}")
        }
    }

    /**
     * Delete a single chat message from Firestore subcollection path jobs/jobId/chats/messageId
     */
    suspend fun deleteChatMessageFromCloud(jobId: String, messageId: String) {
        if (!isFirebaseInitialized()) return
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection("jobs")
                .document(jobId)
                .collection("chats")
                .document(messageId)
                .delete()
                .awaitTask()
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error deleting chat message from cloud: ${e.localizedMessage}")
        }
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

    /**
     * Fetch user profile from Firestore once.
     */
    suspend fun fetchUserProfile(userId: String): UserProfile? {
        if (!isFirebaseInitialized()) return null
        return try {
            val db = FirebaseFirestore.getInstance()
            kotlinx.coroutines.withTimeout(4000) {
                val document = db.collection("users").document(userId).get().awaitTask()
                if (document.exists()) {
                    document.toObject(UserProfile::class.java)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error fetching user profile from Firestore: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Fetch user profile from Firestore by email.
     */
    suspend fun fetchUserProfileByEmail(email: String): UserProfile? {
        if (!isFirebaseInitialized()) return null
        return try {
            val db = FirebaseFirestore.getInstance()
            kotlinx.coroutines.withTimeout(4000) {
                val querySnapshot = db.collection("users")
                    .whereEqualTo("email", email.trim().lowercase())
                    .limit(1)
                    .get()
                    .awaitTask()
                val document = querySnapshot.documents.firstOrNull()
                if (document != null && document.exists()) {
                    document.toObject(UserProfile::class.java)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error fetching user profile by email from Firestore: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Real-time subscription to a user profile in Firestore.
     */
    fun listenToUserProfile(userId: String, onProfileChanged: (UserProfile) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        if (!isFirebaseInitialized()) return null
        return try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FirebaseService", "Listen to user profile failed", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val profile = snapshot.toObject(UserProfile::class.java)
                        if (profile != null) {
                            onProfileChanged(profile)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error setting up profile listener: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Real-time subscription to jobs visible for current user.
     * It merges:
     * - jobs where user is client
     * - jobs where user is professional
     * - open jobs (optional, for professionals marketplace feed)
     */

    /**
     * Fetch Departments and Municipalities from Firestore.
     * Seeds them if empty.
     */
    suspend fun fetchDepartments(): List<FirebaseDepartment> {
        if (!isFirebaseInitialized()) return emptyList()
        return try {
            val db = FirebaseFirestore.getInstance()
            val collectionRef = db.collection("departments")
            kotlinx.coroutines.withTimeout(4000) {
                val snapshot = collectionRef.get().awaitTask()
                
                if (snapshot.isEmpty) {
                    val seeds = listOf(
                        FirebaseDepartment("Bogotá D.C.", listOf("Bogotá")),
                        FirebaseDepartment("Antioquia", listOf("Medellín", "Envigado", "Bello", "Rionegro")),
                        FirebaseDepartment("Valle del Cauca", listOf("Cali", "Palmira", "Yumbo", "Jamundí")),
                        FirebaseDepartment("Atlántico", listOf("Barranquilla", "Soledad", "Puerto Colombia")),
                        FirebaseDepartment("Santander", listOf("Bucaramanga", "Floridablanca", "Girón"))
                    )
                    for (dept in seeds) {
                        collectionRef.document(dept.name).set(dept).awaitTask()
                    }
                    seeds
                } else {
                    snapshot.toObjects(FirebaseDepartment::class.java)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error fetching/seeding departments: ${e.localizedMessage}")
            emptyList()
        }
    }

    /**
     * Fetch Categories of Work from Firestore.
     * Seeds them if empty.
     */
    suspend fun fetchCategories(): List<FirebaseCategory> {
        if (!isFirebaseInitialized()) return emptyList()
        return try {
            val db = FirebaseFirestore.getInstance()
            val collectionRef = db.collection("categories")
            kotlinx.coroutines.withTimeout(4000) {
                val snapshot = collectionRef.get().awaitTask()
                
                if (snapshot.isEmpty) {
                    val seeds = listOf(
                        FirebaseCategory("Plomería"),
                        FirebaseCategory("Electricidad"),
                        FirebaseCategory("Carpintería"),
                        FirebaseCategory("Pintura"),
                        FirebaseCategory("Albañilería"),
                        FirebaseCategory("Jardinería"),
                        FirebaseCategory("Cerrajería"),
                        FirebaseCategory("Fumigación")
                    )
                    for (cat in seeds) {
                        collectionRef.document(cat.name).set(cat).awaitTask()
                    }
                    seeds
                } else {
                    snapshot.toObjects(FirebaseCategory::class.java)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error fetching/seeding categories: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun addCategory(name: String): Boolean {
        if (!isFirebaseInitialized()) return false
        return try {
            val db = FirebaseFirestore.getInstance()
            val collectionRef = db.collection("categories")
            val cat = FirebaseCategory(name)
            kotlinx.coroutines.withTimeout(4000) {
                collectionRef.document(name).set(cat).awaitTask()
            }
            true
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error adding category: ${e.localizedMessage}")
            false
        }
    }
}


