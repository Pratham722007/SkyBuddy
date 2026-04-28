package com.example.skybuddy.di

import com.example.skybuddy.audio.AndroidSpeechRecognizerImpl
import com.example.skybuddy.audio.AndroidTextToSpeech
import com.example.skybuddy.audio.SpeechRecognizer
import com.example.skybuddy.audio.TextToSpeechService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {
    @Binds @Singleton
    abstract fun bindSpeechRecognizer(impl: AndroidSpeechRecognizerImpl): SpeechRecognizer

    @Binds @Singleton
    abstract fun bindTextToSpeech(impl: AndroidTextToSpeech): TextToSpeechService
}
