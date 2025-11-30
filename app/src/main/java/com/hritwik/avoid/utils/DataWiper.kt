package com.hritwik.avoid.utils

import android.content.Context
import com.hritwik.avoid.data.cache.CacheManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.VoidDatabase
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataWiper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val cacheManager: CacheManager
) {
    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        preferencesManager.clearAllPlaybackPositions()
        val database = VoidDatabase.getDatabase(context)
        database.searchResultDao().clearAll()
        database.clearDatabase()

        cacheManager.clearAll()

        UserDataViewModel.deleteDir(context.cacheDir)
        context.externalCacheDir?.let { UserDataViewModel.deleteDir(it) }

        UserDataViewModel.deleteDir(File(context.filesDir, "downloads"))
        context.getExternalFilesDir(null)?.let { UserDataViewModel.deleteDir(File(it, "downloads")) }

        UserDataViewModel.deleteDir(File(context.cacheDir, "temp"))
        UserDataViewModel.deleteDir(File(context.filesDir, "temp"))
        context.getExternalFilesDir(null)?.let { UserDataViewModel.deleteDir(File(it, "temp")) }
    }
}