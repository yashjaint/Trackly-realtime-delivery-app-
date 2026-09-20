package com.trackly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trackly.core.database.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderByIdFlow(orderId: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE status NOT IN ('DELIVERED', 'CANCELLED') ORDER BY createdAt DESC LIMIT 1")
    fun getActiveOrderFlow(): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE status NOT IN ('DELIVERED', 'CANCELLED') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveOrder(): OrderEntity?

    @Query("SELECT * FROM orders WHERE status NOT IN ('DELIVERED', 'CANCELLED') ORDER BY createdAt DESC")
    suspend fun getAllActiveOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt, isSynced = :isSynced WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String, updatedAt: Long, isSynced: Boolean = true)

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrder(orderId: String)

    @Query("DELETE FROM orders")
    suspend fun clearAllOrders()
}
