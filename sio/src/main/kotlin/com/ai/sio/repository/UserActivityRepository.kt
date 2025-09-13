package com.ai.sio.repository

import com.ai.sio.entity.ActivityType
import com.ai.sio.entity.User
import com.ai.sio.entity.UserActivity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserActivityRepository : JpaRepository<UserActivity, Long> {
    
    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<UserActivity>
    
    fun countByActivityTypeAndCreatedAtBetween(
        activityType: ActivityType, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): Long
    
    fun findByUserAndCreatedAtBetween(
        user: User, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): List<UserActivity>
}