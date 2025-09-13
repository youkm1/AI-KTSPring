package com.ai.sio.dto.feedback

import com.ai.sio.entity.FeedbackStatus
import com.ai.sio.entity.UserFeedback
import java.time.LocalDateTime

data class FeedbackDto(
    val id: Long,
    val userId: Long,
    val userName: String,
    val userEmail: String,
    val threadId: Long,
    val isPositive: Boolean,
    val status: FeedbackStatus,
    val comment: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(feedback: UserFeedback): FeedbackDto {
            return FeedbackDto(
                id = feedback.id,
                userId = feedback.user.id,
                userName = feedback.user.name,
                userEmail = feedback.user.email,
                threadId = feedback.chatThread.id,
                isPositive = feedback.isPositive,
                status = feedback.status,
                comment = feedback.comment,
                createdAt = feedback.createdAt,
                updatedAt = feedback.updatedAt
            )
        }
    }
}

data class CreateFeedbackRequest(
    val threadId: Long,
    val isPositive: Boolean,
    val comment: String? = null
)

data class UpdateFeedbackStatusRequest(
    val status: FeedbackStatus
)