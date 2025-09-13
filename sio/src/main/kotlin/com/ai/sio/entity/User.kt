package com.ai.sio.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true, nullable = false)
    val email: String = "",
    
    @Column(nullable = false)
    val password: String = "",
    
    @Column(nullable = false)
    val name: String = "",
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.MEMBER
) : TimeStampEntity()
