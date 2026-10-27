package com.galibu.core.data.fake

import com.galibu.core.data.local.ChatMessageDao
import com.galibu.core.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeChatMessageDao : ChatMessageDao {

    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    override fun getMessagesForJobFlow(jobId: String): Flow<List<ChatMessage>> =
        messages.map { list -> list.filter { it.jobId == jobId } }

    override fun getAllMessagesFlow(): Flow<List<ChatMessage>> =
        messages.map { list -> list.sortedByDescending { it.timestamp } }

    override suspend fun insertMessage(message: ChatMessage) {
        messages.value = messages.value.filter { it.id != message.id } + message
    }

    override suspend fun markMessagesAsRead(jobId: String, currentUserId: String) {
        messages.value = messages.value.map { message ->
            if (message.jobId == jobId && message.senderId != currentUserId) {
                message.copy(read = true)
            } else {
                message
            }
        }
    }

    override suspend fun deleteAllChatMessages() {
        messages.value = emptyList()
    }
}
