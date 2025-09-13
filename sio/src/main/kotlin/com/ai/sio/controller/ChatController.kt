package com.ai.sio.controller

import com.ai.sio.dto.chat.ChatHistoryResponse
import com.ai.sio.dto.chat.MessageResponse
import com.ai.sio.dto.chat.SendMessageRequest
import com.ai.sio.dto.chat.ThreadResponse
import com.ai.sio.entity.User
import com.ai.sio.service.ChatService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {
    
    @PostMapping("/threads")
    fun createThread(@AuthenticationPrincipal user: User): ResponseEntity<ThreadResponse> {
        val thread = chatService.createThread(user)
        return ResponseEntity.ok(
            ThreadResponse(
                id = thread.id,
                createdAt = thread.createdAt,
                updatedAt = thread.updatedAt
            )
        )
    }
    
    @GetMapping("/threads")
    fun getUserThreads(@AuthenticationPrincipal user: User): ResponseEntity<List<ThreadResponse>> {
        val threads = chatService.getUserThreads(user)
        val response = threads.map { thread ->
            ThreadResponse(
                id = thread.id,
                createdAt = thread.createdAt,
                updatedAt = thread.updatedAt
            )
        }
        return ResponseEntity.ok(response)
    }
    
    @DeleteMapping("/threads/{threadId}")
    fun deleteThread(
        @PathVariable threadId: Long,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Void> {
        chatService.deleteThread(threadId, user)
        return ResponseEntity.noContent().build()
    }
    
    @PostMapping("/threads/{threadId}/messages")
    fun sendMessage(
        @PathVariable threadId: Long,
        @RequestBody request: SendMessageRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<MessageResponse> {
        val message = chatService.sendMessage(threadId, request.content, user)
        return ResponseEntity.ok(
            MessageResponse(
                id = message.id,
                role = message.role.name,
                content = message.content,
                createdAt = message.createdAt
            )
        )
    }
    
    @GetMapping("/threads/{threadId}/messages")
    fun getMessages(
        @PathVariable threadId: Long,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ChatHistoryResponse> {
        val messages = chatService.getMessages(threadId, user)
        val thread = messages.firstOrNull()?.thread
        
        if (thread == null) {
            return ResponseEntity.notFound().build()
        }
        
        val response = ChatHistoryResponse(
            thread = ThreadResponse(
                id = thread.id,
                createdAt = thread.createdAt,
                updatedAt = thread.updatedAt
            ),
            messages = messages.map { message ->
                MessageResponse(
                    id = message.id,
                    role = message.role.name,
                    content = message.content,
                    createdAt = message.createdAt
                )
            }
        )
        return ResponseEntity.ok(response)
    }
}