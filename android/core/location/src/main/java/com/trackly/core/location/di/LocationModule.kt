package com.trackly.core.location.di

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.trackly.core.location.LocationClient
import com.trackly.core.location.LocationClientImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideLocationClient(
        @ApplicationContext context: Context
    ): LocationClient {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return LocationClientImpl(context, fusedLocationClient)
    }
}
