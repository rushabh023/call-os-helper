package com.example.helper_application.dialer.di

import android.content.Context
import com.example.helper_application.dialer.feature.recordings.RecordingManager
import com.example.helper_application.dialer.feature.settings.DialerRecordingPreferences
import com.example.helper_application.recording.CallRecorder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRecordingManager(
        @ApplicationContext context: Context
    ): RecordingManager = RecordingManager(context)

    @Provides
    @Singleton
    fun provideCallRecorder(
        @ApplicationContext context: Context
    ): CallRecorder = CallRecorder(context)

    @Provides
    @Singleton
    fun provideDialerRecordingPreferences(
        @ApplicationContext context: Context
    ): DialerRecordingPreferences = DialerRecordingPreferences(context)
}
