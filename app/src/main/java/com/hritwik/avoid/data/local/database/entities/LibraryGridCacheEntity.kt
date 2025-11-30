package com.hritwik.avoid.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey




@Entity(
    tableName = "library_grid_cache",
    indices = [
        Index(value = ["libraryKey", "indexInSort"], unique = true),
        Index(value = ["libraryKey"])
    ]
)
data class LibraryGridCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val libraryKey: String,
    val indexInSort: Int,
    val mediaId: String,
    val name: String,
    val type: String,
    val primaryImageTag: String?,
    val backdropImageTag: String?,
    val progress: Float,
    val runTimeTicks: Long?
)
