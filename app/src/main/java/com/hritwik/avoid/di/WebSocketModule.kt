package com.hritwik.avoid.di

import com.hritwik.avoid.data.remote.websocket.PlaybackEventParser
import com.hritwik.avoid.data.repository.ContinueWatchingStore
import com.hritwik.avoid.data.repository.NextUpStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
object WebSocketModule {
    @Provides
    @Singleton
    fun providePlaybackEventParser(): PlaybackEventParser = PlaybackEventParser()

    @Provides
    @Singleton
    fun provideContinueWatchingStore(@WebSocketScope scope: CoroutineScope): ContinueWatchingStore =
        ContinueWatchingStore(scope)

    @Provides
    @Singleton
    fun provideNextUpStore(@WebSocketScope scope: CoroutineScope): NextUpStore =
        NextUpStore(scope)
}
