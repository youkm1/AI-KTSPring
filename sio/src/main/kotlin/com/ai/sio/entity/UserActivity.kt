package com.ai.sio.entity

import jakarta.persistence.*

@Entity
class UserActivity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val activityType: ActivityType = ActivityType.REGISTER,
    
    @Column(columnDefinition = "TEXT")
    val details: String? = null
) : BaseEntity()