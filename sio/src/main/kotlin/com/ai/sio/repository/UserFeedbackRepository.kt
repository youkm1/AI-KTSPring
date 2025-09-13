package com.ai.sio.repository

import com.ai.sio.entity.ChatThread
import com.ai.sio.entity.FeedbackStatus
import com.ai.sio.entity.User
import com.ai.sio.entity.UserFeedback
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserFeedbackRepository : JpaRepository<UserFeedback, Long> {
    
    fun findAllByOrderByCreatedAtAsc(pageable: Pageable): Page<UserFeedback>
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<UserFeedback>
    
    fun findByIsPositiveOrderByCreatedAtAsc(isPositive: Boolean, pageable: Pageable): Page<UserFeedback>
    fun findByIsPositiveOrderByCreatedAtDesc(isPositive: Boolean, pageable: Pageable): Page<UserFeedback>
    
    fun findByStatusOrderByCreatedAtAsc(status: FeedbackStatus, pageable: Pageable): Page<UserFeedback>
    fun findByStatusOrderByCreatedAtDesc(status: FeedbackStatus, pageable: Pageable): Page<UserFeedback>
    
    fun findByIsPositiveAndStatusOrderByCreatedAtAsc(
        isPositive: Boolean, 
        status: FeedbackStatus, 
        pageable: Pageable
    ): Page<UserFeedback>
    
    fun findByIsPositiveAndStatusOrderByCreatedAtDesc(
        isPositive: Boolean, 
        status: FeedbackStatus, 
        pageable: Pageable
    ): Page<UserFeedback>
    
    fun findByUserAndChatThread(user: User, chatThread: ChatThread): Optional<UserFeedback>
    fun existsByUserAndChatThread(user: User, chatThread: ChatThread): Boolean
}