package com.example.skysecurity.di

import android.content.Context
import com.example.skybuddy.shared.data.repository.MapRepository
import com.example.skybuddy.shared.location.BlockedRegionManager
import com.example.skybuddy.shared.location.IndoorLocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideMapRepository(@ApplicationContext context: Context): MapRepository {
        return MapRepository(context)
    }

    @Provides
    @Singleton
    fun provideIndoorLocationManager(): IndoorLocationManager {
        return IndoorLocationManager()
    }

    @Provides
    @Singleton
    fun provideBlockedRegionManager(): BlockedRegionManager {
        return BlockedRegionManager()
    }
}
