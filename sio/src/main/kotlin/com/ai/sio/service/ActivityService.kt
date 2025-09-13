package com.ai.sio.service

import com.ai.sio.dto.admin.ActivityStatsDto
import com.ai.sio.entity.ActivityType
import com.ai.sio.entity.User
import com.ai.sio.entity.UserActivity
import com.ai.sio.repository.UserActivityRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class ActivityService(
    private val userActivityRepository: UserActivityRepository
) {
    
    fun recordActivity(user: User, activityType: ActivityType, details: String? = null) {
        val activity = UserActivity(
            user = user,
            activityType = activityType,
            details = details
        )
        userActivityRepository.save(activity)
    }
    
    fun getDailyActivityStats(targetDate: LocalDate): ActivityStatsDto {
        val startOfDay = targetDate.atStartOfDay()
        val endOfDay = targetDate.atTime(23, 59, 59)
        
        val registerCount = userActivityRepository.countByActivityTypeAndCreatedAtBetween(
            ActivityType.REGISTER, startOfDay, endOfDay
        )
        
        val loginCount = userActivityRepository.countByActivityTypeAndCreatedAtBetween(
            ActivityType.LOGIN, startOfDay, endOfDay
        )
        
        val chatCreatedCount = userActivityRepository.countByActivityTypeAndCreatedAtBetween(
            ActivityType.CHAT_CREATED, startOfDay, endOfDay
        )
        
        return ActivityStatsDto(
            date = targetDate,
            registerCount = registerCount,
            loginCount = loginCount,
            chatCreatedCount = chatCreatedCount
        )
    }
}