package com.trackly.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trackly.core.database.dao.OrderDao
import com.trackly.core.database.dao.PendingActionDao
import com.trackly.core.database.entity.OrderEntity
import com.trackly.core.database.entity.PendingActionEntity

@Database(
    entities = [
        OrderEntity::class,
        PendingActionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TracklyDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun pendingActionDao(): PendingActionDao
}
