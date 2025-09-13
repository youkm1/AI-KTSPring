package com.ai.sio.controller

import com.ai.sio.dto.admin.ActivityStatsDto
import com.ai.sio.dto.feedback.FeedbackDto
import com.ai.sio.dto.feedback.UpdateFeedbackStatusRequest
import com.ai.sio.entity.FeedbackStatus
import com.ai.sio.service.ActivityService
import com.ai.sio.service.FeedbackService
import com.ai.sio.service.ReportService
import org.springframework.data.domain.Page
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val feedbackService: FeedbackService,
    private val activityService: ActivityService,
    private val reportService: ReportService
) {
    
    @GetMapping("/feedback")
    fun getFeedbacks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "desc") sort: String,
        @RequestParam(required = false) isPositive: Boolean?,
        @RequestParam(required = false) status: FeedbackStatus?
    ): ResponseEntity<Page<FeedbackDto>> {
        
        val feedbacks = feedbackService.getFeedbacks(page, size, sort, isPositive, status)
        return ResponseEntity.ok(feedbacks)
    }
    
    @PutMapping("/feedback/{feedbackId}/status")
    fun updateFeedbackStatus(
        @PathVariable feedbackId: Long,
        @RequestBody request: UpdateFeedbackStatusRequest
    ): ResponseEntity<FeedbackDto> {
        
        val updatedFeedback = feedbackService.updateFeedbackStatus(feedbackId, request.status)
        return ResponseEntity.ok(updatedFeedback)
    }
    
    @GetMapping("/activity-stats")
    fun getActivityStats(
        @RequestParam(required = false) date: LocalDate?
    ): ResponseEntity<ActivityStatsDto> {
        
        val targetDate = date ?: LocalDate.now()
        val stats = activityService.getDailyActivityStats(targetDate)
        return ResponseEntity.ok(stats)
    }
    
    @GetMapping("/reports/daily-chats")
    fun downloadDailyChatReport(
        @RequestParam(required = false) date: LocalDate?
    ): ResponseEntity<ByteArray> {
        
        val targetDate = date ?: LocalDate.now()
        val csvData = reportService.generateDailyChatReport(targetDate)
        
        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType("text/csv")
            setContentDispositionFormData("attachment", "daily-chat-report-$targetDate.csv")
        }
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(csvData)
    }
}