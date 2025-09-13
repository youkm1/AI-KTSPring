package com.ai.sio.service

import com.ai.sio.component.AIClient
import com.ai.sio.dto.AIMessage
import com.ai.sio.dto.redis.RedisMessage
import com.ai.sio.dto.redis.ThreadMetadata
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisChatService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val chatPersistenceService: ChatPersistenceService,
    private val aiClient: AIClient
) {
    
    private val objectMapper = ObjectMapper()
    private val TTL_MINUTES = 30L
    
    fun sendMessage(userId: Long, message: String): RedisMessage {
        val threadId = getOrCreateActiveThread(userId)
        
        val userMessage = RedisMessage(
            role = "user",
            content = message,
            timestamp = System.currentTimeMillis()
        )
        
        addMessageToRedis(threadId, userMessage, userId)
        updateThreadTTL(threadId)
        
        val conversationHistory = getConversationFromRedis(threadId)
        
        val aiResponse = aiClient.generateContent(
            conversationHistory.map { 
                AIMessage(role = it.role, content = it.content) 
            }
        ).block() ?: "응답을 생성할 수 없습니다"
        
        val aiMessage = RedisMessage(
            role = "assistant",
            content = aiResponse,
            timestamp = System.currentTimeMillis()
        )
        
        addMessageToRedis(threadId, aiMessage, userId)
        updateThreadTTL(threadId)
        
        return aiMessage
    }
    
    private fun addMessageToRedis(threadId: String, message: RedisMessage, userId: Long) {
        val key = "thread:$threadId:messages"
        
        redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(message))
        redisTemplate.expire(key, Duration.ofMinutes(TTL_MINUTES))
        
        updateThreadMetadata(threadId, userId)
    }
    
    private fun updateThreadTTL(threadId: String) {
        val messagesKey = "thread:$threadId:messages"
        val metadataKey = "thread:$threadId:metadata"
        
        redisTemplate.expire(messagesKey, Duration.ofMinutes(TTL_MINUTES))
        redisTemplate.expire(metadataKey, Duration.ofMinutes(TTL_MINUTES))
        
        println("Thread $threadId TTL updated to ${TTL_MINUTES} minutes")
    }
    
    private fun updateThreadMetadata(threadId: String, userId: Long) {
        val metadataKey = "thread:$threadId:metadata"
        val existingJson = redisTemplate.opsForValue().get(metadataKey) as? String
        
        val metadata = if (existingJson != null) {
            val existing = objectMapper.readValue(existingJson, ThreadMetadata::class.java)
            existing.copy(lastUpdatedAt = System.currentTimeMillis())
        } else {
            ThreadMetadata(
                threadId = threadId,
                userId = userId,
                startedAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis()
            )
        }
        
        redisTemplate.opsForValue().set(
            metadataKey,
            objectMapper.writeValueAsString(metadata),
            Duration.ofMinutes(TTL_MINUTES)
        )
    }
    
    fun getConversationFromRedis(threadId: String): List<RedisMessage> {
        val key = "thread:$threadId:messages"
        val messageJsonList = redisTemplate.opsForList().range(key, 0, -1)
        
        return messageJsonList?.mapNotNull { json ->
            try {
                objectMapper.readValue(json.toString(), RedisMessage::class.java)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }
    
    private fun getOrCreateActiveThread(userId: Long): String {
        val activeThread = findActiveThreadForUser(userId)
        
        return activeThread ?: run {
            val newThreadId = "thread_${userId}_${System.currentTimeMillis()}"
            println("Created new thread: $newThreadId for user: $userId")
            newThreadId
        }
    }
    
    private fun findActiveThreadForUser(userId: Long): String? {
        val pattern = "thread:thread_${userId}_*:metadata"
        val keys = redisTemplate.keys(pattern)
        
        return keys?.firstOrNull()?.let { key ->
            key.removePrefix("thread:").removeSuffix(":metadata")
        }
    }
    
    fun completeConversation(userId: Long) {
        val activeThreadId = findActiveThreadForUser(userId)
        activeThreadId?.let { threadId ->
            moveToPostgreSQL(threadId)
            cleanupRedisThread(threadId)
        }
    }
    
    fun moveToPostgreSQL(threadId: String) {
        val messages = getConversationFromRedis(threadId)
        val metadataKey = "thread:$threadId:metadata"
        val metadataJson = redisTemplate.opsForValue().get(metadataKey) as? String
        
        if (messages.isNotEmpty() && metadataJson != null) {
            val metadata = objectMapper.readValue(metadataJson, ThreadMetadata::class.java)
            
            chatPersistenceService.saveConversation(metadata, messages)
            
            println("Moved thread $threadId to PostgreSQL (${messages.size} messages)")
        }
    }
    
    private fun cleanupRedisThread(threadId: String) {
        redisTemplate.delete("thread:$threadId:messages")
        redisTemplate.delete("thread:$threadId:metadata")
    }
    
    fun getActiveThreadId(userId: Long): String? {
        return findActiveThreadForUser(userId)
    }
}