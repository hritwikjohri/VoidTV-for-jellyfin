package com.hritwik.avoid.utils

import android.content.Context
import java.io.File

object MpvConfig {
    private const val MPV_DIRECTORY = "mpv"
    private const val CONFIG_FILE_NAME = "mpv.conf"

    private fun configDirectory(context: Context): File {
        return File(context.filesDir, MPV_DIRECTORY)
    }

    fun ensureConfig(context: Context): File {
        val directory = configDirectory(context).apply { if (!exists()) mkdirs() }
        val configFile = File(directory, CONFIG_FILE_NAME)
        if (!configFile.exists()) {
            configFile.writeText(defaultConfig())
        }
        return configFile
    }

    fun readConfig(context: Context): String {
        val file = ensureConfig(context)
        return runCatching { file.readText() }.getOrDefault(defaultConfig())
    }

    fun writeConfig(context: Context, content: String): String {
        val file = ensureConfig(context)
        val normalized = normalizeContent(content)
        file.writeText(normalized)
        return normalized
    }

    fun readOptions(context: Context): Map<String, String> =
        parseOptions(readConfig(context))

    fun parseOptions(content: String): Map<String, String> {
        val options = mutableMapOf<String, String>()
        content.lineSequence().forEach { rawLine ->
            val withoutComment = rawLine.substringBefore('#').trim()
            if (withoutComment.isEmpty()) return@forEach

            val delimiterIndex = withoutComment.indexOf('=').takeIf { it >= 0 }
                ?: withoutComment.indexOf(' ').takeIf { it >= 0 }

            val key: String
            val value: String
            if (delimiterIndex == null || delimiterIndex < 0) {
                key = withoutComment.lowercase()
                value = "yes"
            } else {
                key = withoutComment.substring(0, delimiterIndex).trim().lowercase()
                value = withoutComment.substring(delimiterIndex + 1).trim()
            }
            if (key.isNotEmpty()) {
                options[key] = value
            }
        }
        return options
    }

    private fun defaultConfig(): String {
        val pageSize = RuntimeConfig.pageSize
        return (
            """
            hwdec=mediacodec,mediacodec-copy
            vo=gpu-next
            sub-codepage=auto
            sub-fix-timing=yes
            blend-subtitles=yes
            sub-forced-only=no
            hwdec-codecs=h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1
            gpu-context=android
            opengl-es=yes 
            ao=audiotrack,opensles
            """.trimIndent() + "\n"
            )
    }

    private fun normalizeContent(content: String): String {
        val normalized = content.replace("\r\n", "\n")
        val trimmed = normalized.trimEnd()
        return if (trimmed.isEmpty()) "" else "$trimmed\n"
    }
}
