package com.ai.sio.entity

import jakarta.persistence.*

@Entity
class UserActivity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val activityType: ActivityType,
    
    @Column(columnDefinition = "TEXT")
    val details: String? = null,
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
) : TimeStampEntity()