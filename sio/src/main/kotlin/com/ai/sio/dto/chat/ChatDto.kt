package com.ai.sio.dto.chat

import java.time.LocalDateTime

data class SendMessageRequest(
    val content: String
)

data class MessageResponse(
    val id: Long,
    val role: String,
    val content: String,
    val createdAt: LocalDateTime
)

data class ThreadResponse(
    val id: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ChatHistoryResponse(
    val thread: ThreadResponse,
    val messages: List<MessageResponse>
)