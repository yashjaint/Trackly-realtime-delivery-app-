package com.trackly.features.auth.service

import com.trackly.core.security.JwtConfig
import com.trackly.core.security.PasswordHasher
import com.trackly.features.auth.domain.DriversTable
import com.trackly.features.auth.domain.UsersTable
import com.trackly.features.auth.dto.AuthResponse
import com.trackly.features.auth.dto.LoginRequest
import com.trackly.features.auth.dto.RegisterRequest
import com.trackly.features.auth.dto.UpdateProfileRequest
import com.trackly.features.auth.dto.UserDto
import com.trackly.features.order.domain.OrderStatusHistoryTable
import com.trackly.features.order.domain.OrdersTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
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
            throw IllegalArgumentException("No account found with this email. Please check your email or Sign Up.")
        }

        val storedHash = userRow[UsersTable.passwordHash]
        val isPasswordValid = try {
            PasswordHasher.verify(request.password, storedHash)
        } catch (e: Throwable) {
            logger.warn("Error verifying password hash for email={}", request.email, e)
            false
        }

        if (!isPasswordValid) {
            logger.warn("Login failed: Password mismatch for email={}", request.email)
            throw IllegalArgumentException("Incorrect password. Please verify your password and try again.")
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

    fun updateUserProfile(userIdStr: String, request: UpdateProfileRequest): UserDto {
        val userId = UUID.fromString(userIdStr)
        logger.info("Updating profile for userId={}", userId)

        transaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[name] = request.name.trim()
            }
            if (!request.vehicleNumber.isNullOrBlank()) {
                DriversTable.update({ DriversTable.userId eq userId }) {
                    it[vehicleNumber] = request.vehicleNumber.trim()
                }
            }
        }

        return getUserProfile(userIdStr)
    }

    fun deleteUserAccount(userIdStr: String) {
        val userId = UUID.fromString(userIdStr)
        logger.info("Requested account deletion for userId={}", userId)

        val userRow = transaction {
            UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
        } ?: throw IllegalArgumentException("User not found")

        val role = userRow[UsersTable.role]

        // Validate active orders requirement: cannot delete account if an active delivery exists
        transaction {
            val activeStatuses = listOf("CREATED", "CONFIRMED", "PREPARING", "READY_FOR_PICKUP", "PICKED_UP", "OUT_FOR_DELIVERY")
            if (role.equals("CUSTOMER", ignoreCase = true)) {
                val hasActiveOrders = OrdersTable.selectAll()
                    .where { (OrdersTable.customerId eq userId) and (OrdersTable.status inList activeStatuses) }
                    .count() > 0
                if (hasActiveOrders) {
                    throw IllegalStateException("Cannot delete account: You have active order(s) in progress. Please wait until your orders are delivered or cancelled.")
                }
            } else if (role.equals("DRIVER", ignoreCase = true)) {
                val driverRow = DriversTable.selectAll().where { DriversTable.userId eq userId }.singleOrNull()
                if (driverRow != null) {
                    val driverId = driverRow[DriversTable.id]
                    val hasActiveDelivery = OrdersTable.selectAll()
                        .where { (OrdersTable.driverId eq driverId) and (OrdersTable.status inList activeStatuses) }
                        .count() > 0
                    if (hasActiveDelivery) {
                        throw IllegalStateException("Cannot delete account: You have an active delivery job in progress. Please complete or cancel all active deliveries first.")
                    }
                }
            }
        }

        // Proceed to delete user profile and driver record cleanly
        transaction {
            // Delete order status history records updated by this user
            OrderStatusHistoryTable.deleteWhere { OrderStatusHistoryTable.updatedBy eq userId }

            // Delete status history and orders created by this user (customer)
            val customerOrderIds = OrdersTable.selectAll()
                .where { OrdersTable.customerId eq userId }
                .map { it[OrdersTable.id] }

            if (customerOrderIds.isNotEmpty()) {
                OrderStatusHistoryTable.deleteWhere { OrderStatusHistoryTable.orderId inList customerOrderIds }
                OrdersTable.deleteWhere { OrdersTable.customerId eq userId }
            }

            // Unassign/delete driver orders and driver record
            val driverRow = DriversTable.selectAll().where { DriversTable.userId eq userId }.singleOrNull()
            if (driverRow != null) {
                val driverId = driverRow[DriversTable.id]
                val driverOrderIds = OrdersTable.selectAll()
                    .where { OrdersTable.driverId eq driverId }
                    .map { it[OrdersTable.id] }
                if (driverOrderIds.isNotEmpty()) {
                    OrderStatusHistoryTable.deleteWhere { OrderStatusHistoryTable.orderId inList driverOrderIds }
                    OrdersTable.deleteWhere { OrdersTable.driverId eq driverId }
                }
                DriversTable.deleteWhere { DriversTable.id eq driverId }
            }

            UsersTable.deleteWhere { UsersTable.id eq userId }
        }

        logger.info("User account successfully deleted: userId={}", userId)
    }
}
