package com.ai.sio.listener

import com.ai.sio.service.RedisChatService
import org.springframework.context.event.EventListener
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.stereotype.Component

@Component
class RedisExpirationListener(
    private val redisChatService: RedisChatService,
    listenerContainer: RedisMessageListenerContainer
) : KeyExpirationEventMessageListener(listenerContainer) {
    
    override fun onMessage(message: Message, pattern: ByteArray?) {
        val expiredKey = message.toString()
        
        if (expiredKey.startsWith("thread:") && expiredKey.endsWith(":messages")) {
            val threadId = expiredKey.removePrefix("thread:").removeSuffix(":messages")
            
            println("Thread $threadId expired, moving to PostgreSQL")
            
            try {
                redisChatService.moveToPostgreSQL(threadId)
            } catch (e: Exception) {
                println("Failed to move expired thread $threadId: ${e.message}")
            }
        }
    }
}