package com.galibu.core.data.fake

import com.galibu.core.data.local.UserProfileDao
import com.galibu.core.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeUserProfileDao : UserProfileDao {

    private val users = MutableStateFlow<List<UserProfile>>(emptyList())

    override fun getUserByIdFlow(id: String): Flow<UserProfile?> =
        users.map { list -> list.find { it.id == id } }

    override suspend fun getUserByIdSync(id: String): UserProfile? =
        users.value.find { it.id == id }

    override suspend fun getUserByEmailSync(email: String): UserProfile? =
        users.value.find { it.email == email }

    override suspend fun insertUser(user: UserProfile) {
        users.value = users.value.filter { it.id != user.id } + user
    }

    override suspend fun updateUser(user: UserProfile) {
        users.value = users.value.map { if (it.id == user.id) user else it }
    }

    override suspend fun deleteUserById(id: String) {
        users.value = users.value.filter { it.id != id }
    }

    override suspend fun deleteProfessionalsNotIn(ids: List<String>, currentUserId: String) {
        users.value = users.value.filter {
            (it.id in ids || it.id == currentUserId) ||
            (it.role != "PROFESSIONAL" && it.role != "BOTH")
        }
    }

    override suspend fun deleteAllProfessionalsExcept(currentUserId: String) {
        users.value = users.value.filter {
            it.id == currentUserId || (it.role != "PROFESSIONAL" && it.role != "BOTH")
        }
    }

    override fun getProfessionalsFlow(): Flow<List<UserProfile>> =
        users.map { list ->
            list.filter { it.role == "PROFESSIONAL" || it.role == "BOTH" }
        }

    override suspend fun deleteAllUserProfiles() {
        users.value = emptyList()
    }
}
