package com.ai.sio.controller

import com.ai.sio.dto.chat.*
import com.ai.sio.dto.redis.RedisMessage
import com.ai.sio.entity.User
import com.ai.sio.service.ChatService
import com.ai.sio.service.RedisChatService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import kotlin.math.min

// ============================================
// 통합된 ChatController - Redis 기반 + DB 히스토리
// ============================================

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val redisChatService: RedisChatService,
    private val chatService: ChatService
) {
    
    // ============================================
    // 실시간 채팅 (Redis 기반)
    // ============================================
    
    @PostMapping("/message")
    fun sendMessage(
        @RequestBody request: SendMessageRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ChatResponse> {
        
        val aiMessage = redisChatService.sendMessage(user.id, request.content)
        
        return ResponseEntity.ok(
            ChatResponse(
                role = aiMessage.role,
                content = aiMessage.content,
                timestamp = aiMessage.timestamp
            )
        )
    }
    
    @GetMapping("/conversation")
    fun getCurrentConversation(@AuthenticationPrincipal user: User): ResponseEntity<ConversationResponse> {
        val activeThreadId = redisChatService.getActiveThreadId(user.id)
        
        return if (activeThreadId != null) {
            val messages = redisChatService.getConversationFromRedis(activeThreadId)
            ResponseEntity.ok(
                ConversationResponse(
                    threadId = activeThreadId,
                    isActive = true,
                    messages = messages.map { 
                        ChatResponse(it.role, it.content, it.timestamp) 
                    }
                )
            )
        } else {
            ResponseEntity.ok(
                ConversationResponse(
                    threadId = null,
                    isActive = false,
                    messages = emptyList()
                )
            )
        }
    }
    
    @PostMapping("/complete")
    fun completeConversation(@AuthenticationPrincipal user: User): ResponseEntity<CompletionResponse> {
        val activeThreadId = redisChatService.getActiveThreadId(user.id)
        
        if (activeThreadId != null) {
            redisChatService.completeConversation(user.id)
            
            return ResponseEntity.ok(
                CompletionResponse(
                    message = "대화가 완료되어 히스토리에 저장되었습니다",
                    threadId = activeThreadId
                )
            )
        } else {
            return ResponseEntity.ok(
                CompletionResponse(
                    message = "진행 중인 대화가 없습니다",
                    threadId = null
                )
            )
        }
    }
    
    // ============================================
    // 히스토리 조회 (PostgreSQL 기반)
    // ============================================
    
    @GetMapping("/history")
    fun getChatHistory(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<HistoryListResponse> {
        
        val threads = chatService.getUserThreads(user)
        
        // 간단한 페이징 처리
        val startIndex = page * size
        val endIndex = min(startIndex + size, threads.size)
        val pagedThreads = if (startIndex < threads.size) {
            threads.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        val response = HistoryListResponse(
            threads = pagedThreads.map { thread ->
                val messageCount = chatService.getMessageCount(thread.id)
                val lastMessage = chatService.getLastMessage(thread.id)
                
                ThreadSummary(
                    id = thread.id,
                    messageCount = messageCount,
                    lastMessage = lastMessage?.take(50), // 미리보기 50자
                    createdAt = thread.createdAt,
                    updatedAt = thread.updatedAt
                )
            },
            currentPage = page,
            totalPages = (threads.size + size - 1) / size,
            totalElements = threads.size
        )
        
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/history/{threadId}")
    fun getThreadDetails(
        @PathVariable threadId: Long,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ThreadDetailResponse> {
        
        val messages = chatService.getMessages(threadId, user)
        val thread = messages.firstOrNull()?.thread
        
        if (thread == null) {
            return ResponseEntity.notFound().build()
        }
        
        val response = ThreadDetailResponse(
            thread = ThreadInfo(
                id = thread.id,
                createdAt = thread.createdAt,
                updatedAt = thread.updatedAt,
                messageCount = messages.size
            ),
            messages = messages.map { message ->
                MessageDetail(
                    id = message.id,
                    role = message.role?.name?.lowercase() ?: "unknown",
                    content = message.content,
                    createdAt = message.createdAt
                )
            }
        )
        
        return ResponseEntity.ok(response)
    }
    
    @DeleteMapping("/history/{threadId}")
    fun deleteThread(
        @PathVariable threadId: Long,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Unit> {
        chatService.deleteThread(threadId, user)
        return ResponseEntity.noContent().build()
    }
    
    // ============================================
    // 상태 조회
    // ============================================
    
    @GetMapping("/status")
    fun getChatStatus(@AuthenticationPrincipal user: User): ResponseEntity<ChatStatusResponse> {
        val activeThreadId = redisChatService.getActiveThreadId(user.id)
        val totalHistoryCount = chatService.getUserThreads(user).size
        
        return ResponseEntity.ok(
            ChatStatusResponse(
                hasActiveConversation = activeThreadId != null,
                activeThreadId = activeThreadId,
                totalHistoryCount = totalHistoryCount
            )
        )
    }
}