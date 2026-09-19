package com.trackly.features.driver.service

import com.trackly.features.auth.domain.DriversTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class DriverService {
    private val logger = LoggerFactory.getLogger(DriverService::class.java)

    fun updateLocation(userIdStr: String, lat: Double, lng: Double): Boolean {
        val userId = UUID.fromString(userIdStr)
        logger.debug("Updating location for driver userId={}: lat={}, lng={}", userIdStr, lat, lng)

        val (driverId, activeOrderId) = transaction {
            val driverRow = DriversTable.selectAll().where { DriversTable.userId eq userId }.singleOrNull()
                ?: return@transaction null to null

            val dId = driverRow[DriversTable.id]

            DriversTable.update({ DriversTable.id eq dId }) {
                it[currentLat] = lat
                it[currentLng] = lng
                it[lastLocationUpdate] = LocalDateTime.now()
            }

            val activeOrderRow = com.trackly.features.order.domain.OrdersTable.selectAll()
                .where { (com.trackly.features.order.domain.OrdersTable.driverId eq dId) and (com.trackly.features.order.domain.OrdersTable.status neq "DELIVERED") and (com.trackly.features.order.domain.OrdersTable.status neq "CANCELLED") }
                .singleOrNull()

            val oId = activeOrderRow?.get(com.trackly.features.order.domain.OrdersTable.id)?.toString()
            dId.toString() to oId
        } ?: return false

        if (activeOrderId != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                com.trackly.features.tracking.service.TrackingSessionManager.broadcastLocation(activeOrderId, driverId, lat, lng)
            }
        }
        return true
    }
}
