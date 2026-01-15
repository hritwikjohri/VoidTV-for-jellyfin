package com.hritwik.avoid.domain.model.playback

enum class HdrFormatPreference(val value: String, val displayName: String) {
    AUTO("auto", "Auto"),
    HDR10_PLUS("hdr10_plus", "HDR10+ only"),
    DOLBY_VISION("dolby_vision", "Dolby Vision only");

    companion object {
        fun fromValue(value: String?): HdrFormatPreference {
            if (value.isNullOrBlank()) {
                return AUTO
            }
            return when (value) {
                "dolby_vision_mel" -> DOLBY_VISION
                else -> entries.firstOrNull { it.value == value } ?: AUTO
            }
        }
    }
}
