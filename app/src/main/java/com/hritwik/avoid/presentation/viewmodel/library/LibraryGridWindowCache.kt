package com.hritwik.avoid.presentation.viewmodel.library

import com.hritwik.avoid.data.local.database.entities.LibraryGridCacheEntity




class LibraryGridWindowCache(
    private val windowSize: Int = 120,
    private val maxEntries: Int = 360 
) {
    private val entries = LinkedHashMap<Int, com.hritwik.avoid.domain.model.library.MediaItem>(maxEntries, 0.75f, true)

    fun get(index: Int): com.hritwik.avoid.domain.model.library.MediaItem? = entries[index]

    fun putAll(offset: Int, data: List<com.hritwik.avoid.domain.model.library.MediaItem>) {
        data.forEachIndexed { idx, item ->
            entries[offset + idx] = item
        }
        trim()
    }

    fun windowFor(index: Int): IntRange {
        val start = (index / windowSize) * windowSize
        return start until (start + windowSize)
    }

    fun clear() = entries.clear()

    fun snapshot(): Map<Int, com.hritwik.avoid.domain.model.library.MediaItem> = HashMap(entries)

    private fun trim() {
        while (entries.size > maxEntries) {
            val firstKey = entries.entries.firstOrNull()?.key ?: break
            entries.remove(firstKey)
        }
    }
}

fun LibraryGridCacheEntity.toMediaItem(): com.hritwik.avoid.domain.model.library.MediaItem {
    return com.hritwik.avoid.domain.model.library.MediaItem(
        id = mediaId,
        name = name,
        title = name,
        type = type,
        overview = null,
        year = null,
        communityRating = null,
        runTimeTicks = runTimeTicks,
        primaryImageTag = primaryImageTag,
        thumbImageTag = null,
        logoImageTag = null,
        backdropImageTags = listOfNotNull(backdropImageTag),
        genres = emptyList(),
        isFolder = false,
        childCount = null,
        userData = null,
        taglines = emptyList(),
        people = emptyList(),
        mediaSources = emptyList(),
        hasSubtitles = false,
        versionName = null,
        seriesName = null,
        seriesId = null,
        seriesPrimaryImageTag = null,
        seasonId = null,
        seasonName = null,
        seasonPrimaryImageTag = null,
        parentIndexNumber = null,
        indexNumber = null,
        tvdbId = null
    )
}
