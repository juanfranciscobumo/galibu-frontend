package com.galibu.core.data.fake

import com.galibu.core.data.local.AppNotificationDao
import com.galibu.core.data.model.AppNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAppNotificationDao : AppNotificationDao {

    private val notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private var autoIncrementId = 0

    override fun getNotificationsForUserFlow(userId: String): Flow<List<AppNotification>> =
        notifications.map { list -> list.filter { it.userId == userId } }

    override suspend fun insertNotification(notification: AppNotification) {
        val id = if (notification.id == 0) ++autoIncrementId else notification.id
        notifications.value = notifications.value.filter { it.id != id } + notification.copy(id = id)
    }

    override suspend fun markAsRead(id: Int) {
        notifications.value = notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    override suspend fun markJobAsRead(userId: String, jobId: String) {
        notifications.value = notifications.value.map {
            if (it.userId == userId && it.jobId == jobId) it.copy(isRead = true) else it
        }
    }

    override suspend fun deleteAllNotifications() {
        notifications.value = emptyList()
    }
}
