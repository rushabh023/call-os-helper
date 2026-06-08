package com.example.dialer.di

import android.content.Context
import androidx.room.Room
import com.example.dialer.core.data.local.DialerDatabase
import com.example.dialer.core.data.local.dao.BlockedNumberDao
import com.example.dialer.core.data.local.dao.CallRecordingDao
import com.example.dialer.core.data.local.dao.SpeedDialDao
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
    fun provideDatabase(@ApplicationContext context: Context): DialerDatabase =
        Room.databaseBuilder(context, DialerDatabase::class.java, "dialer.db").build()

    @Provides
    fun provideCallRecordingDao(db: DialerDatabase): CallRecordingDao = db.callRecordingDao()

    @Provides
    fun provideBlockedNumberDao(db: DialerDatabase): BlockedNumberDao = db.blockedNumberDao()

    @Provides
    fun provideSpeedDialDao(db: DialerDatabase): SpeedDialDao = db.speedDialDao()
}
