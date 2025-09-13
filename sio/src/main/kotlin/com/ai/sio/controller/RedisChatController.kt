package com.ai.sio.controller

import com.ai.sio.dto.redis.RedisMessage
import com.ai.sio.entity.User
import com.ai.sio.service.RedisChatService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/redis-chat")
class RedisChatController(
    private val redisChatService: RedisChatService
) {
    
    @PostMapping("/message")
    fun sendMessage(
        @RequestBody request: ChatRequest,
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
    
    @PostMapping("/complete")
    fun completeConversation(@AuthenticationPrincipal user: User): ResponseEntity<Unit> {
        redisChatService.completeConversation(user.id)
        return ResponseEntity.ok().build()
    }
    
    @GetMapping("/conversation")
    fun getCurrentConversation(@AuthenticationPrincipal user: User): ResponseEntity<List<RedisMessage>> {
        val activeThreadId = redisChatService.getActiveThreadId(user.id)
        
        return if (activeThreadId != null) {
            val messages = redisChatService.getConversationFromRedis(activeThreadId)
            ResponseEntity.ok(messages)
        } else {
            ResponseEntity.ok(emptyList())
        }
    }
}

data class ChatRequest(val content: String)
data class ChatResponse(
    val role: String,
    val content: String,
    val timestamp: Long
)