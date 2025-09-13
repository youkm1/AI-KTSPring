package com.ai.sio.controller

import com.ai.sio.dto.feedback.CreateFeedbackRequest
import com.ai.sio.dto.feedback.FeedbackDto
import com.ai.sio.entity.User
import com.ai.sio.service.FeedbackService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/feedback")
class FeedbackController(
    private val feedbackService: FeedbackService
) {
    
    @PostMapping
    fun createFeedback(
        @RequestBody request: CreateFeedbackRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<FeedbackDto> {
        
        val feedback = feedbackService.createFeedback(
            user = user,
            threadId = request.threadId,
            isPositive = request.isPositive,
            comment = request.comment
        )
        
        return ResponseEntity.ok(FeedbackDto.from(feedback))
    }
}