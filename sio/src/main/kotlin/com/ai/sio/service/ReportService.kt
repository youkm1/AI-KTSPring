package com.ai.sio.service

import com.ai.sio.repository.ChatThreadRepository
import org.springframework.stereotype.Service
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ReportService(
    private val chatThreadRepository: ChatThreadRepository
) {
    
    fun generateDailyChatReport(targetDate: LocalDate): ByteArray {
        val startOfDay = targetDate.atStartOfDay()
        val endOfDay = targetDate.atTime(23, 59, 59)
        
        val threads = chatThreadRepository.findByCreatedAtBetween(startOfDay, endOfDay)
        
        val csvContent = buildString {
            appendLine("Thread ID,User ID,User Name,User Email,Created At,Updated At")
            
            threads.forEach { thread ->
                val user = thread.user
                appendLine(
                    "${thread.id}," +
                    "${user?.id ?: "N/A"}," +
                    "\"${user?.name ?: "N/A"}\"," +
                    "\"${user?.email ?: "N/A"}\"," +
                    "\"${thread.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}\"," +
                    "\"${thread.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}\""
                )
            }
        }
        
        return csvContent.toByteArray(Charset.forName("UTF-8"))
    }
}