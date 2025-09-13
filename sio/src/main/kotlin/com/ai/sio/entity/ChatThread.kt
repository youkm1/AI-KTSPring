package com.ai.sio.entity

import jakarta.persistence.*

@Entity
class ChatThread(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User? = null
): BaseEntity()
