package com.ai.sio.config.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtUtil {
    
    @Value("\${jwt.secret:mySecretKey}")
    private lateinit var secret: String
    
    @Value("\${jwt.expiration:86400000}") // 24 hours
    private val expiration: Long = 86400000
    
    private fun getSigningKey(): Key = Keys.hmacShaKeyFor(secret.toByteArray())
    
    fun generateToken(email: String, role: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)
        
        return Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact()
    }
    
    fun getEmailFromToken(token: String): String {
        return getClaims(token).subject
    }
    
    fun getRoleFromToken(token: String): String {
        return getClaims(token)["role"] as String
    }
    
    fun isTokenValid(token: String): Boolean {
        try {
            val claims = getClaims(token)
            return !claims.expiration.before(Date())
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun getClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .body
    }
}