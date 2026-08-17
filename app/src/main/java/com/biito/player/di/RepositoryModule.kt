package com.biito.player.di

import com.biito.player.data.repository.LocalMediaRepository
import com.biito.player.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        localMediaRepository: LocalMediaRepository,
    ): MediaRepository
}