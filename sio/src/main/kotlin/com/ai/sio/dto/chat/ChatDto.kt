package com.ai.sio.dto.chat

import java.time.LocalDateTime

// ============================================
// 기존 DTO 클래스들
// ============================================

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

// ============================================
// 새로운 통합 DTO 클래스들
// ============================================

data class ChatResponse(
    val role: String,
    val content: String,
    val timestamp: Long
)

data class ConversationResponse(
    val threadId: String?,
    val isActive: Boolean,
    val messages: List<ChatResponse>
)

data class CompletionResponse(
    val message: String,
    val threadId: String?
)

data class HistoryListResponse(
    val threads: List<ThreadSummary>,
    val currentPage: Int,
    val totalPages: Int,
    val totalElements: Int
)

data class ThreadSummary(
    val id: Long,
    val messageCount: Int,
    val lastMessage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class ThreadDetailResponse(
    val thread: ThreadInfo,
    val messages: List<MessageDetail>
)

data class ThreadInfo(
    val id: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val messageCount: Int
)

data class MessageDetail(
    val id: Long,
    val role: String,
    val content: String,
    val createdAt: LocalDateTime
)

data class ChatStatusResponse(
    val hasActiveConversation: Boolean,
    val activeThreadId: String?,
    val totalHistoryCount: Int
)