package com.example.skybuddy.di

import com.example.skybuddy.ai.LiteRtLlmEngine
import com.example.skybuddy.ai.LlmEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    @Singleton
    abstract fun bindLlmEngine(impl: LiteRtLlmEngine): LlmEngine
}
