package com.hritwik.avoid.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey





@Entity(
    tableName = "library_alpha_index",
    indices = [Index(value = ["libraryKey"], unique = true)]
)
data class LibraryAlphaIndexEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val libraryKey: String,
    val letter: String,
    val firstIndex: Int
)




@Entity(
    tableName = "library_alpha_meta",
    indices = [Index(value = ["libraryKey"], unique = true)]
)
data class LibraryAlphaMetaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val libraryKey: String,
    val totalCount: Int
)
