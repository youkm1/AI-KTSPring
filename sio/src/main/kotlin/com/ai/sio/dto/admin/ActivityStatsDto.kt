package com.ai.sio.dto.admin

import java.time.LocalDate

data class ActivityStatsDto(
    val date: LocalDate,
    val registerCount: Long,
    val loginCount: Long,
    val chatCreatedCount: Long
)