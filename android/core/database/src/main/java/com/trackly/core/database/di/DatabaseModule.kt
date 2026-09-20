package com.trackly.core.database.di

import android.content.Context
import androidx.room.Room
import com.trackly.core.database.TracklyDatabase
import com.trackly.core.database.dao.OrderDao
import com.trackly.core.database.dao.PendingActionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTracklyDatabase(
        @ApplicationContext context: Context
    ): TracklyDatabase {
        return Room.databaseBuilder(
            context,
            TracklyDatabase::class.java,
            "trackly_local.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideOrderDao(database: TracklyDatabase): OrderDao {
        return database.orderDao()
    }

    @Provides
    @Singleton
    fun providePendingActionDao(database: TracklyDatabase): PendingActionDao {
        return database.pendingActionDao()
    }
}
