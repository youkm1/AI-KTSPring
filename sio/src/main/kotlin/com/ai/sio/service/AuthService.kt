package com.ai.sio.service

import com.ai.sio.config.security.JwtUtil
import com.ai.sio.dto.auth.AuthResponse
import com.ai.sio.dto.auth.LoginRequest
import com.ai.sio.dto.auth.RegisterRequest
import com.ai.sio.dto.auth.UserInfo
import com.ai.sio.entity.ActivityType
import com.ai.sio.entity.User
import com.ai.sio.entity.UserRole
import com.ai.sio.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val activityService: ActivityService
) {
    
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("이미 존재하는 이메일입니다")
        }
        
        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            name = request.name,
            role = UserRole.MEMBER
        )
        
        val savedUser = userRepository.save(user)
        
        // 회원가입 활동 기록
        activityService.recordActivity(savedUser, ActivityType.REGISTER, "사용자 회원가입")
        
        val token = jwtUtil.generateToken(savedUser.email, savedUser.role.name)
        
        return AuthResponse(
            token = token,
            user = UserInfo(
                id = savedUser.id,
                email = savedUser.email,
                name = savedUser.name,
                role = savedUser.role.name
            )
        )
    }
    
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { IllegalArgumentException("존재하지 않는 사용자입니다") }
        
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다")
        }
        
        // 로그인 활동 기록
        activityService.recordActivity(user, ActivityType.LOGIN, "사용자 로그인")
        
        val token = jwtUtil.generateToken(user.email, user.role.name)
        
        return AuthResponse(
            token = token,
            user = UserInfo(
                id = user.id,
                email = user.email,
                name = user.name,
                role = user.role.name
            )
        )
    }
}