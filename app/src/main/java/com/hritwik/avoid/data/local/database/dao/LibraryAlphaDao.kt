package com.hritwik.avoid.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hritwik.avoid.data.local.database.entities.LibraryAlphaIndexEntity
import com.hritwik.avoid.data.local.database.entities.LibraryAlphaMetaEntity

@Dao
interface LibraryAlphaDao {

    @Transaction
    suspend fun replaceIndex(
        key: String,
        totalCount: Int,
        entries: List<LibraryAlphaIndexEntity>
    ) {
        deleteByKey(key)
        insertMeta(LibraryAlphaMetaEntity(libraryKey = key, totalCount = totalCount))
        if (entries.isNotEmpty()) {
            insertAll(entries)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LibraryAlphaIndexEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: LibraryAlphaMetaEntity)

    @Query("SELECT id, libraryKey, letter, firstIndex FROM library_alpha_index WHERE libraryKey = :key")
    suspend fun getIndex(key: String): List<LibraryAlphaIndexEntity>

    @Query("SELECT totalCount FROM library_alpha_meta WHERE libraryKey = :key LIMIT 1")
    suspend fun getTotalCount(key: String): Int?

    @Query("DELETE FROM library_alpha_index WHERE libraryKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM library_alpha_meta WHERE libraryKey = :key")
    suspend fun deleteMetaByKey(key: String)
}
