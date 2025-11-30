package com.hritwik.avoid.presentation.ui.components.common

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.media3.common.MediaItem as ExoMediaItem

@OptIn(UnstableApi::class)
@Composable
fun ThemeSongPlayer(url: String?, volumeLevel: Int) {
    val context = LocalContext.current
    val cacheFactory = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerCacheEntryPoint::class.java
        ).cacheDataSourceFactory()
    }
    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .build()
    }

    LaunchedEffect(url, volumeLevel) {
        val normalizedVolume = volumeLevel.coerceIn(1, 10) / 10f
        player.volume = normalizedVolume
        if (url != null) {
            player.setMediaItem(ExoMediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        } else {
            player.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
}

@EntryPoint
@OptIn(UnstableApi::class)
@InstallIn(SingletonComponent::class)
interface PlayerCacheEntryPoint {
    fun cacheDataSourceFactory(): CacheDataSource.Factory
}