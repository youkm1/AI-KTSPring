package com.ai.sio.repository

import com.ai.sio.entity.ChatThread
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatThreadRepository : JpaRepository<ChatThread, Long> {
    fun findByUserIdOrderByUpdatedAtDesc(userId: Long): List<ChatThread>
    fun findByCreatedAtBetween(startDate: java.time.LocalDateTime, endDate: java.time.LocalDateTime): List<ChatThread>
}