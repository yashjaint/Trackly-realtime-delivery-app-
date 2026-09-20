package com.trackly.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey
    val id: String,
    val orderId: String,
    val actionType: String, // UPDATE_STATUS | UPDATE_LOCATION
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
