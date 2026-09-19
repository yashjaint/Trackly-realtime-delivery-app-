package com.trackly.features.auth.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object UsersTable : Table("users") {
    val id = uuid("id").defaultExpression(org.jetbrains.exposed.sql.CustomFunction("random_uuid", org.jetbrains.exposed.sql.UUIDColumnType()))
    val name = varchar("name", 128)
    val email = varchar("email", 256).uniqueIndex()
    val passwordHash = varchar("password_hash", 256)
    val role = varchar("role", 32) // CUSTOMER | DRIVER | ADMIN
    val fcmToken = varchar("fcm_token", 512).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

object DriversTable : Table("drivers") {
    val id = uuid("id").defaultExpression(org.jetbrains.exposed.sql.CustomFunction("random_uuid", org.jetbrains.exposed.sql.UUIDColumnType()))
    val userId = reference("user_id", UsersTable.id)
    val vehicleNumber = varchar("vehicle_number", 64)
    val status = varchar("status", 32).default("OFFLINE") // OFFLINE | AVAILABLE | ON_DELIVERY
    val currentLat = double("current_lat").nullable()
    val currentLng = double("current_lng").nullable()
    val lastLocationUpdate = datetime("last_location_update").nullable()

    override val primaryKey = PrimaryKey(id)
}
