package com.hritwik.avoid.presentation.ui.navigation

import android.net.Uri
import com.hritwik.avoid.domain.model.library.LibraryType

object Routes {
    const val SERVER_SETUP = "server_setup"
    const val LOGIN = "login"
    const val QUICK_CONNECT = "quick_connect"
    const val QUICK_CONNECT_AUTHORIZE = "quick_connect_authorize"
    const val HOME = "home"
    const val MOVIES = "movies"
    const val SHOWS = "shows"
    const val LIBRARY = "library"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val COLLECTIONS = "collections"
    const val COLLECTION_DETAIL = "collection_detail/{collectionId}/{collectionName}"
    const val LIBRARY_DETAIL = "library_detail/{libraryId}/{libraryName}?studio={studio}&additionalLibraryIds={additionalLibraryIds}&libraryType={libraryType}"
    const val MEDIA_DETAIL = "media_detail/{mediaId}?seasonNumber={seasonNumber}&episodeId={episodeId}"
    const val VIDEO_PLAYER = "video_player/{mediaId}?mediaSourceId={mediaSourceId}&audioStreamIndex={audioStreamIndex}&subtitleStreamIndex={subtitleStreamIndex}&startPosition={startPosition}"
    fun mediaDetail(
        mediaId: String,
        seasonNumber: Int? = null,
        episodeId: String? = null
    ): String {
        return buildString {
            append("media_detail/$mediaId")
            if (seasonNumber != null || episodeId != null) {
                append("?")
                seasonNumber?.let {
                    append("seasonNumber=$it")
                }
                episodeId?.let {
                    if (seasonNumber != null) append("&")
                    append("episodeId=$it")
                }
            }
        }
    }
    fun collectionDetail(
        collectionId: String,
        collectionName: String
    ): String {
        val encodedId = Uri.encode(collectionId)
        val encodedName = Uri.encode(collectionName)
        return "collection_detail/$encodedId/$encodedName"
    }
    fun libraryDetail(
        libraryId: String,
        libraryName: String,
        studio: String? = null,
        additionalLibraryIds: List<String> = emptyList(),
        libraryType: LibraryType? = null
    ): String {
        val encodedId = Uri.encode(libraryId)
        val encodedName = Uri.encode(libraryName)
        val base = "library_detail/$encodedId/$encodedName"
        val queryParams = buildList {
            studio?.takeIf { it.isNotBlank() }?.let { add("studio=${Uri.encode(it)}") }
            if (additionalLibraryIds.isNotEmpty()) {
                val encodedIds = additionalLibraryIds
                    .joinToString(",") { Uri.encode(it) }
                add("additionalLibraryIds=${Uri.encode(encodedIds)}")
            }
            libraryType?.let { add("libraryType=${Uri.encode(it.name)}") }
        }
        return if (queryParams.isEmpty()) base else "$base?${queryParams.joinToString("&")}"
    }
    fun videoPlayer(
        mediaId: String,
        mediaSourceId: String? = null,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        startPosition: Long = 0
    ): String {
        return buildString {
            append("video_player/$mediaId")
            append("?mediaSourceId=${mediaSourceId ?: ""}")
            append("&audioStreamIndex=${audioStreamIndex ?: -1}")
            append("&subtitleStreamIndex=${subtitleStreamIndex ?: -1}")
            append("&startPosition=$startPosition")
        }
    }
}
