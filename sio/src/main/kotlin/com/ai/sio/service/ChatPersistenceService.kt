package com.ai.sio.service

import com.ai.sio.dto.redis.RedisMessage
import com.ai.sio.dto.redis.ThreadMetadata
import com.ai.sio.entity.ActivityType
import com.ai.sio.entity.ChatMessage
import com.ai.sio.entity.ChatThread
import com.ai.sio.entity.MessageRole
import com.ai.sio.repository.ChatMessageRepository
import com.ai.sio.repository.ChatThreadRepository
import com.ai.sio.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Transactional
class ChatPersistenceService(
    private val userRepository: UserRepository,
    private val chatThreadRepository: ChatThreadRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val activityService: ActivityService
) {
    
    fun saveConversation(metadata: ThreadMetadata, messages: List<RedisMessage>) {
        val user = userRepository.findById(metadata.userId)
            .orElseThrow { EntityNotFoundException("User not found") }
        
        val chatThread = ChatThread(user = user)
        val savedThread = chatThreadRepository.save(chatThread)
        
        val chatMessages = messages.map { redisMessage ->
            ChatMessage(
                thread = savedThread,
                role = when(redisMessage.role) {
                    "user" -> MessageRole.USER
                    "assistant" -> MessageRole.ASSISTANT
                    else -> MessageRole.USER
                },
                content = redisMessage.content
            )
        }
        
        chatMessageRepository.saveAll(chatMessages)
        
        println("Saved ${chatMessages.size} messages for thread ${savedThread.id}")
    }
}