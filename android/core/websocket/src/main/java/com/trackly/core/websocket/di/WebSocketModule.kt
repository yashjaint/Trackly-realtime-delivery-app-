package com.trackly.core.websocket.di

import com.trackly.core.websocket.TrackingWebSocketClient
import com.trackly.core.websocket.TrackingWebSocketClientImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WebSocketModule {

    @Binds
    @Singleton
    abstract fun bindTrackingWebSocketClient(
        impl: TrackingWebSocketClientImpl
    ): TrackingWebSocketClient
}
