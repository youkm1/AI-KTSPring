package com.ai.sio.service

import com.ai.sio.component.AIClient
import com.ai.sio.dto.AIMessage
import com.ai.sio.entity.*
import com.ai.sio.repository.ChatMessageRepository
import com.ai.sio.repository.ChatThreadRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import java.nio.file.AccessDeniedException
import java.time.Duration

@Service
@Transactional
class ChatService(
    private val chatThreadRepository: ChatThreadRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val aiClient: AIClient,
    private val activityService: ActivityService
) {
    fun sendMessage(threadId: Long, userMessage: String, user: User): ChatMessage {
        val thread = chatThreadRepository.findById(threadId)
            .orElseThrow { EntityNotFoundException("Thread not found") }

        if (thread.user?.id != user.id) {
            throw AccessDeniedException("접근 권한이 없습니다")
        }

        val userChatMessage = ChatMessage(
            thread = thread,
            role = MessageRole.USER,
            content = userMessage
        )
        chatMessageRepository.save(userChatMessage)

        val conversationHistory = buildConversationHistory(threadId)

        val aiResponse = aiClient.generateContent(conversationHistory)
            .block(Duration.ofSeconds(30))
            ?: throw RuntimeException("AI 응답을 받을 수 없습니다")

        val aiChatMessage = ChatMessage(
            thread = thread,
            role = MessageRole.ASSISTANT,
            content = aiResponse
        )

        return chatMessageRepository.save(aiChatMessage)
    }

    private fun buildConversationHistory(threadId: Long): List<AIMessage> {
        val messages = chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)

        return messages.map { message ->
            AIMessage(
                role = when(message.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "system"
                    else -> "user"
                },
                content = message.content
            )
        }
    }

    fun createThread(user: User): ChatThread {
        val thread = ChatThread(user = user)
        val savedThread = chatThreadRepository.save(thread)
        
        // 대화 생성 활동 기록
        activityService.recordActivity(user, ActivityType.CHAT_CREATED, "새 대화 스레드 생성")
        
        return savedThread
    }

    fun getMessages(threadId: Long, user: User): List<ChatMessage> {
        val thread = chatThreadRepository.findById(threadId)
            .orElseThrow { EntityNotFoundException("Thread not found") }

        if (thread.user?.id != user.id && user.role != UserRole.ADMIN) {
            throw AccessDeniedException("접근 권한이 없습니다")
        }

        return chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)
    }

    fun getUserThreads(user: User): List<ChatThread> {
        return chatThreadRepository.findByUserIdOrderByUpdatedAtDesc(user.id)
    }

    fun deleteThread(threadId: Long, user: User) {
        val thread = chatThreadRepository.findById(threadId)
            .orElseThrow { EntityNotFoundException("Thread not found") }

        if (thread.user?.id != user.id && user.role != UserRole.ADMIN) {
            throw AccessDeniedException("접근 권한이 없습니다")
        }

        chatThreadRepository.delete(thread)
    }
    
    // ============================================
    // 통합 컨트롤러를 위한 추가 메서드들
    // ============================================
    
    fun getMessageCount(threadId: Long): Int {
        return chatMessageRepository.countByThreadId(threadId)
    }
    
    fun getLastMessage(threadId: Long): String? {
        val messages = chatMessageRepository.findByThreadIdOrderByCreatedAtDesc(threadId)
        return messages.firstOrNull()?.content
    }

}