package com.ai.sio.dto.redis

import java.util.*

data class RedisMessage(
    val role: String,
    val content: String,
    val timestamp: Long,
    val messageId: String = UUID.randomUUID().toString()
)

data class ThreadMetadata(
    val threadId: String,
    val userId: Long,
    val startedAt: Long,
    val lastUpdatedAt: Long,
    val status: String = "ACTIVE"
)