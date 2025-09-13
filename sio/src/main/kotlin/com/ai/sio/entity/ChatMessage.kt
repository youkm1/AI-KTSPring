package com.ai.sio.entity

import jakarta.persistence.*

@Entity
class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    val thread: ChatThread? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: MessageRole? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String = ""
) : TimeStampEntity()