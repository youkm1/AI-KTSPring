package com.ai.sio.entity

import jakarta.persistence.*

@Entity
class UserFeedback(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    val chatThread: ChatThread,
    
    @Column(name = "is_positive", nullable = false)
    val isPositive: Boolean,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: FeedbackStatus = FeedbackStatus.PENDING,
    
    @Column(columnDefinition = "TEXT")
    val comment: String? = null,
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
) : TimeStampEntity()