package com.ai.sio.repository

import com.ai.sio.entity.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    fun findByThreadIdOrderByCreatedAtAsc(threadId: Long): List<ChatMessage>
}