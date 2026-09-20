package com.trackly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trackly.core.database.entity.PendingActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun getAllPendingActions(): List<PendingActionEntity>

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    fun getAllPendingActionsFlow(): Flow<List<PendingActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingAction(action: PendingActionEntity)

    @Query("DELETE FROM pending_actions WHERE id = :actionId")
    suspend fun deletePendingAction(actionId: String)

    @Query("DELETE FROM pending_actions")
    suspend fun clearAllPendingActions()
}
