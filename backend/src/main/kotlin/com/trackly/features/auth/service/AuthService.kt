package com.trackly.features.auth.service

import com.trackly.core.security.JwtConfig
import com.trackly.core.security.PasswordHasher
import com.trackly.features.auth.domain.DriversTable
import com.trackly.features.auth.domain.UsersTable
import com.trackly.features.auth.dto.AuthResponse
import com.trackly.features.auth.dto.LoginRequest
import com.trackly.features.auth.dto.RegisterRequest
import com.trackly.features.auth.dto.UserDto
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.UUID

class AuthService {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun register(request: RegisterRequest): AuthResponse {
        logger.info("Attempting to register user: email={}, role={}", request.email, request.role)

        val existingUser = transaction {
            UsersTable.selectAll().where { UsersTable.email eq request.email.lowercase().trim() }.singleOrNull()
        }

        if (existingUser != null) {
            logger.warn("Registration rejected: email {} already exists", request.email)
            throw IllegalArgumentException("User with email '${request.email}' already exists")
        }

        val hashedPassword = PasswordHasher.hash(request.password)
        val newUserId = UUID.randomUUID()

        transaction {
            UsersTable.insert {
                it[id] = newUserId
                it[name] = request.name.trim()
                it[email] = request.email.lowercase().trim()
                it[passwordHash] = hashedPassword
                it[role] = request.role.uppercase().trim()
            }

            if (request.role.equals("DRIVER", ignoreCase = true)) {
                DriversTable.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = newUserId
                    it[vehicleNumber] = request.vehicleNumber ?: "UNKNOWN-VEHICLE"
                    it[status] = "OFFLINE"
                }
                logger.info("Driver profile record created for userId={}", newUserId)
            }
        }

        logger.info("User registered successfully: userId={}, email={}", newUserId, request.email)

        val token = JwtConfig.generateToken(newUserId, request.email.lowercase().trim(), request.role.uppercase().trim())
        val userDto = UserDto(
            id = newUserId.toString(),
            name = request.name,
            email = request.email.lowercase().trim(),
            role = request.role.uppercase().trim()
        )

        return AuthResponse(token, userDto)
    }

    fun login(request: LoginRequest): AuthResponse {
        logger.info("Attempting login for email: {}", request.email)

        val userRow = transaction {
            UsersTable.selectAll().where { UsersTable.email eq request.email.lowercase().trim() }.singleOrNull()
        } ?: run {
            logger.warn("Login failed: User not found for email={}", request.email)
            throw IllegalArgumentException("Invalid email or password")
        }

        val storedHash = userRow[UsersTable.passwordHash]
        if (!PasswordHasher.verify(request.password, storedHash)) {
            logger.warn("Login failed: Password mismatch for email={}", request.email)
            throw IllegalArgumentException("Invalid email or password")
        }

        val userId = userRow[UsersTable.id]
        val email = userRow[UsersTable.email]
        val role = userRow[UsersTable.role]
        val name = userRow[UsersTable.name]
        val fcmToken = userRow[UsersTable.fcmToken]

        logger.info("Login successful for userId={}, role={}", userId, role)

        val token = JwtConfig.generateToken(userId, email, role)
        val userDto = UserDto(
            id = userId.toString(),
            name = name,
            email = email,
            role = role,
            fcmToken = fcmToken
        )

        return AuthResponse(token, userDto)
    }

    fun getUserProfile(userIdStr: String): UserDto {
        val userId = UUID.fromString(userIdStr)
        val userRow = transaction {
            UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
        } ?: throw IllegalArgumentException("User not found")

        return UserDto(
            id = userRow[UsersTable.id].toString(),
            name = userRow[UsersTable.name],
            email = userRow[UsersTable.email],
            role = userRow[UsersTable.role],
            fcmToken = userRow[UsersTable.fcmToken]
        )
    }
}
