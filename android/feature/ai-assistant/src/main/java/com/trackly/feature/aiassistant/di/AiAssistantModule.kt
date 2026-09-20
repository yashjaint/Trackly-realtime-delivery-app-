package com.trackly.feature.aiassistant.di

import com.trackly.feature.aiassistant.data.repository.AiAssistantRepositoryImpl
import com.trackly.feature.aiassistant.domain.repository.AiAssistantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiAssistantModule {

    @Binds
    @Singleton
    abstract fun bindAiAssistantRepository(
        impl: AiAssistantRepositoryImpl
    ): AiAssistantRepository
}
