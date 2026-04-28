package com.databridgepro.filemanager.di

import android.content.Context
import androidx.room.Room
import com.databridgepro.filemanager.data.local.AppDatabase
import com.databridgepro.filemanager.data.local.BackupDao
import com.databridgepro.filemanager.data.repository.FileRepository
import com.databridgepro.filemanager.data.repository.FileRepositoryImpl
import dagger.Binds
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "databridgepro.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBackupDao(db: AppDatabase): BackupDao = db.backupDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository
}
