package com.seuusuario.silentalert.core.di

import android.content.Context
import androidx.room.Room
import com.seuusuario.silentalert.data.local.db.AlertDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): AlertDatabase =
        Room.databaseBuilder(context, AlertDatabase::class.java, "alert_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideContactDao(db: AlertDatabase) = db.contactDao()
}
