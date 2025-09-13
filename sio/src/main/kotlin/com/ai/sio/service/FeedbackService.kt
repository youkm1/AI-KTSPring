package com.ai.sio.service

import com.ai.sio.dto.feedback.FeedbackDto
import com.ai.sio.entity.FeedbackStatus
import com.ai.sio.entity.User
import com.ai.sio.entity.UserFeedback
import com.ai.sio.repository.ChatThreadRepository
import com.ai.sio.repository.UserFeedbackRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.nio.file.AccessDeniedException

@Service
@Transactional
class FeedbackService(
    private val userFeedbackRepository: UserFeedbackRepository,
    private val chatThreadRepository: ChatThreadRepository
) {
    
    fun createFeedback(
        user: User, 
        threadId: Long, 
        isPositive: Boolean, 
        comment: String?
    ): UserFeedback {
        val chatThread = chatThreadRepository.findById(threadId)
            .orElseThrow { EntityNotFoundException("Thread not found") }
        
        if (chatThread.user?.id != user.id) {
            throw AccessDeniedException("본인의 대화에만 피드백을 남길 수 있습니다")
        }
        
        if (userFeedbackRepository.existsByUserAndChatThread(user, chatThread)) {
            throw IllegalStateException("이미 이 대화에 피드백을 남겼습니다")
        }
        
        val feedback = UserFeedback(
            user = user,
            chatThread = chatThread,
            isPositive = isPositive,
            comment = comment,
            status = FeedbackStatus.PENDING
        )
        
        return userFeedbackRepository.save(feedback)
    }
    
    fun getFeedbacks(
        page: Int = 0,
        size: Int = 20,
        sortDirection: String = "desc",
        isPositive: Boolean? = null,
        status: FeedbackStatus? = null
    ): Page<FeedbackDto> {
        
        val pageable = PageRequest.of(page, size)
        val isAscending = sortDirection.lowercase() == "asc"
        
        val feedbackPage = when {
            isPositive != null && status != null -> {
                if (isAscending) {
                    userFeedbackRepository.findByIsPositiveAndStatusOrderByCreatedAtAsc(isPositive, status, pageable)
                } else {
                    userFeedbackRepository.findByIsPositiveAndStatusOrderByCreatedAtDesc(isPositive, status, pageable)
                }
            }
            isPositive != null -> {
                if (isAscending) {
                    userFeedbackRepository.findByIsPositiveOrderByCreatedAtAsc(isPositive, pageable)
                } else {
                    userFeedbackRepository.findByIsPositiveOrderByCreatedAtDesc(isPositive, pageable)
                }
            }
            status != null -> {
                if (isAscending) {
                    userFeedbackRepository.findByStatusOrderByCreatedAtAsc(status, pageable)
                } else {
                    userFeedbackRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                }
            }
            else -> {
                if (isAscending) {
                    userFeedbackRepository.findAllByOrderByCreatedAtAsc(pageable)
                } else {
                    userFeedbackRepository.findAllByOrderByCreatedAtDesc(pageable)
                }
            }
        }
        
        return feedbackPage.map { FeedbackDto.from(it) }
    }
    
    fun updateFeedbackStatus(feedbackId: Long, newStatus: FeedbackStatus): FeedbackDto {
        val feedback = userFeedbackRepository.findById(feedbackId)
            .orElseThrow { EntityNotFoundException("Feedback not found") }
        
        val updatedFeedback = UserFeedback(
            user = feedback.user,
            chatThread = feedback.chatThread,
            isPositive = feedback.isPositive,
            comment = feedback.comment,
            status = newStatus,
            id = feedback.id
        )
        
        val saved = userFeedbackRepository.save(updatedFeedback)
        return FeedbackDto.from(saved)
    }
}