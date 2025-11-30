package com.hritwik.avoid.data.remote.dto.library

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StudioDto(
    @SerialName("Id")
    val id: String? = null,
    @SerialName("Name")
    val name: String? = null,
    @SerialName("ImageTags")
    val imageTags: ImageTags? = null
)
