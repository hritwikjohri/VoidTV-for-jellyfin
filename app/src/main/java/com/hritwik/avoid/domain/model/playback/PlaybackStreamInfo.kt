package com.hritwik.avoid.domain.model.playback

import com.hritwik.avoid.domain.model.media.MediaStream

data class PlaybackStreamInfo(
    val url: String,
    val playSessionId: String? = null,
    val mediaStreams: List<MediaStream> = emptyList(),
)
