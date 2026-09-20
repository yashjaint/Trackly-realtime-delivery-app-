package com.trackly.core.database

import com.trackly.features.auth.domain.DriversTable
import com.trackly.features.auth.domain.UsersTable
import com.trackly.features.order.domain.OrderStatusHistoryTable
import com.trackly.features.order.domain.OrdersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun init() {
        logger.info("Initializing Trackly database connection...")

        val config = HikariConfig().apply {
            driverClassName = System.getenv("DB_DRIVER") ?: "org.h2.Driver"
            jdbcUrl = System.getenv("DB_URL") ?: "jdbc:h2:file:./build/trackly_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
            username = System.getenv("DB_USER") ?: "sa"
            password = System.getenv("DB_PASSWORD") ?: ""
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            logger.info("Creating or updating database schemas for Users, Drivers, Orders, and OrderStatusHistory tables...")
            SchemaUtils.createMissingTablesAndColumns(UsersTable, DriversTable, OrdersTable, OrderStatusHistoryTable)
        }

        logger.info("Database initialization completed successfully.")
    }
}
