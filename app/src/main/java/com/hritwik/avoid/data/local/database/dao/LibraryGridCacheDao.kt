package com.hritwik.avoid.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hritwik.avoid.data.local.database.entities.LibraryGridCacheEntity

@Dao
interface LibraryGridCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LibraryGridCacheEntity>)

    @Query("DELETE FROM library_grid_cache WHERE libraryKey = :key")
    suspend fun deleteByKey(key: String)

    @Transaction
    @Query(
        "SELECT * FROM library_grid_cache WHERE libraryKey = :key AND indexInSort BETWEEN :start AND :end ORDER BY indexInSort ASC"
    )
    suspend fun getWindow(key: String, start: Int, end: Int): List<LibraryGridCacheEntity>
}
