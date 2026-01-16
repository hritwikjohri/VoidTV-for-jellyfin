package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.playback.DisplayMode
import com.hritwik.avoid.domain.model.playback.PlaybackTranscodeOption
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun VersionSelectionDialog(
    versions: List<MediaSource>,
    selectedVersion: MediaSource?,
    onVersionSelected: (MediaSource) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Select Version",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            items(versions, key = { it.id }) { version ->
                val isSelected = selectedVersion?.id == version.id
                val focusRequester = remember { FocusRequester() }

                SelectionItem(
                    title = version.displayName,
                    subtitle = buildVersionSubtitle(version),
                    isSelected = isSelected,
                    onClick = {
                        onVersionSelected(version)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }
        }
    }
}

private fun buildVersionSubtitle(version: MediaSource): String {
    val videoStream = version.defaultVideoStream
    val codecText = formatVideoCodecLabel(videoStream?.codec)
    val profileText = formatVideoProfile(videoStream?.profile, videoStream?.codec)

    return listOfNotNull(
        version.versionInfo,
        version.container?.uppercase(),
        version.size?.let(::formatFileSize),
        version.bitrate?.let(::formatBitrateMbps),
        codecText,
        profileText
    ).joinToString(" • ")
}

private fun formatBitrateMbps(bitrate: Int): String {
    val mbps = bitrate / 1_000_000.0
    return "%.1f Mbps".format(mbps)
}

private fun formatVideoCodecLabel(codec: String?): String? {
    val normalized = codec?.trim()?.lowercase().orEmpty()
    if (normalized.isEmpty()) return null

    return when {
        normalized.startsWith("h264") || normalized == "avc" -> "H.264"
        normalized.startsWith("h265") ||
            normalized == "hevc" ||
            normalized == "hev1" ||
            normalized == "hvc1" -> "HEVC"
        normalized.startsWith("av1") -> "AV1"
        normalized.startsWith("vp9") -> "VP9"
        normalized.startsWith("vp8") -> "VP8"
        else -> normalized.uppercase()
    }
}

private fun formatVideoProfile(profile: String?, codec: String?): String? {
    val normalizedProfile = profile?.trim()?.lowercase() ?: return null
    val normalizedCodec = codec?.trim()?.lowercase().orEmpty()

    val isAvc = normalizedCodec.startsWith("h264") || normalizedCodec == "avc"
    val isHevc = normalizedCodec.startsWith("h265") ||
        normalizedCodec == "hevc" ||
        normalizedCodec == "hev1" ||
        normalizedCodec == "hvc1"

    val isHi10 = normalizedProfile.contains("hi10") ||
        normalizedProfile.contains("high 10") ||
        normalizedProfile.contains("high10")
    val isMain10 = normalizedProfile.contains("main 10") || normalizedProfile.contains("main10")

    return when {
        isHi10 && isAvc -> "High 10"
        isMain10 && isHevc -> "Main 10"
        else -> null
    }
}

@Composable
fun AudioTrackDialog(
    audioStreams: List<MediaStream>,
    selectedAudioStream: MediaStream?,
    onAudioSelected: (MediaStream) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Audio Track",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            items(audioStreams, key = { it.index }) { audioStream ->
                val isSelected = selectedAudioStream?.index == audioStream.index
                val focusRequester = remember { FocusRequester() }

                SelectionItem(
                    title = audioStream.audioDescription,
                    subtitle = buildString {
                        audioStream.codec?.let { append(it.uppercase()) }
                        audioStream.bitRate?.let {
                            if (isNotEmpty()) append(" • ")
                            append("${it / 1000} kbps")
                        }
                        if (audioStream.isDefault) {
                            if (isNotEmpty()) append(" • ")
                            append("Default")
                        }
                    },
                    isSelected = isSelected,
                    onClick = {
                        onAudioSelected(audioStream)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }
        }
    }
}

@Composable
fun SubtitleDialog(
    subtitleStreams: List<MediaStream>,
    selectedSubtitleStream: MediaStream?,
    onSubtitleSelected: (MediaStream?) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Subtitles",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            
            item {
                val isSelected = selectedSubtitleStream == null
                val focusRequester = remember { FocusRequester() }

                SelectionItem(
                    title = "Off",
                    subtitle = "No subtitles",
                    isSelected = isSelected,
                    onClick = {
                        onSubtitleSelected(null)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }

            
            items(subtitleStreams, key = { it.index }) { subtitleStream ->
                val isSelected = selectedSubtitleStream?.index == subtitleStream.index
                val focusRequester = remember { FocusRequester() }

                SelectionItem(
                    title = subtitleStream.subtitleDescription,
                    subtitle = buildString {
                        subtitleStream.codec?.let { append(it.uppercase()) }
                        if (subtitleStream.isDefault) {
                            if (isNotEmpty()) append(" • ")
                            append("Default")
                        }
                        if (subtitleStream.isForced) {
                            if (isNotEmpty()) append(" • ")
                            append("Forced")
                        }
                        if (subtitleStream.isExternal) {
                            if (isNotEmpty()) append(" • ")
                            append("External")
                        }
                    },
                    isSelected = isSelected,
                    onClick = {
                        onSubtitleSelected(subtitleStream)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }
        }
    }
}

@Composable
fun DisplayModeSelectionDialog(
    currentMode: DisplayMode,
    onModeSelected: (DisplayMode) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Display Mode",
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(DisplayMode.entries, key = { it.name }) { mode ->
                val isSelected = currentMode == mode
                val focusRequester = remember { FocusRequester() }

                SelectionItem(
                    title = mode.value,
                    isSelected = isSelected,
                    onClick = {
                        onModeSelected(mode)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }
        }
    }
}

@Composable
fun VideoQualityDialog(
    selectedOption: PlaybackTranscodeOption,
    onSelect: (PlaybackTranscodeOption) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Video Quality",
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(PlaybackTranscodeOption.entries, key = { it.name }) { option ->
                val isSelected = selectedOption == option
                val focusRequester = remember { FocusRequester() }

                SelectionItem(
                    title = option.label,
                    subtitle = when {
                        option.isOriginal -> "Direct stream - no transcoding"
                        else -> buildString {
                            option.displayHeight?.let { append("${it}p") }
                            option.displayBitrate?.let {
                                if (isNotEmpty()) append(" • ")
                                append("${it / 1_000_000} Mbps")
                            }
                        }
                    },
                    isSelected = isSelected,
                    onClick = {
                        onSelect(option)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }
        }
    }
}

@Composable
fun SubtitleOptionsDialog(
    currentOffsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val stepMs = 100L
    val minOffsetMs = -5000L
    val maxOffsetMs = 5000L

    SelectionDialog(
        title = "Subtitle Offset",
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = calculateRoundedValue(8).sdp),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            OffsetButton(
                text = "-",
                onClick = {
                    val newOffset = (currentOffsetMs - stepMs).coerceAtLeast(minOffsetMs)
                    onOffsetChange(newOffset)
                },
                enabled = currentOffsetMs > minOffsetMs,
                modifier = Modifier.weight(1f)
            )

            
            Text(
                text = formatOffsetMs(currentOffsetMs),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.5f)
            )

            
            OffsetButton(
                text = "+",
                onClick = {
                    val newOffset = (currentOffsetMs + stepMs).coerceAtMost(maxOffsetMs)
                    onOffsetChange(newOffset)
                },
                enabled = currentOffsetMs < maxOffsetMs,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OffsetButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isFocused = remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(calculateRoundedValue(12).sdp)
    val focusRequester = remember { FocusRequester() }

    val backgroundColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        isFocused.value -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        isFocused.value -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isFocused.value -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .clip(shape)
            .onFocusChanged { isFocused.value = it.hasFocus }
            .selectable(
                selected = false,
                onClick = { if (enabled) onClick() },
                role = Role.Button
            )
            .focusRequester(focusRequester)
            .focusable()
            .border(
                width = calculateRoundedValue(1).sdp,
                color = borderColor,
                shape = shape
            )
            .background(backgroundColor, shape)
            .padding(
                horizontal = calculateRoundedValue(16).sdp,
                vertical = calculateRoundedValue(12).sdp
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatOffsetMs(offsetMs: Long): String {
    return when {
        offsetMs == 0L -> "0ms"
        offsetMs > 0 -> "+${offsetMs}ms"
        else -> "${offsetMs}ms"
    }
}

@Composable
fun SubtitleMenuDialog(
    onSubtitleOffsetClick: () -> Unit,
    onSubtitleSizeClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Player Options",
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            item {
                SectionLabel(text = "Subtitle Options")
            }
            item {
                SelectionItem(
                    title = "Subtitle Offset",
                    subtitle = "Adjust subtitle timing",
                    isSelected = false,
                    showRadio = false,
                    onClick = {
                        onSubtitleOffsetClick()
                    }
                )
            }
            item {
                SelectionItem(
                    title = "Subtitle Size",
                    subtitle = "Change subtitle text size",
                    isSelected = false,
                    showRadio = false,
                    onClick = {
                        onSubtitleSizeClick()
                    }
                )
            }
            item {
                SectionLabel(text = "Playback Settings")
            }
            item {
                SelectionItem(
                    title = "Playback Speed",
                    subtitle = "Current playback rate: 1x (Default)",
                    isSelected = false,
                    showRadio = false,
                    onClick = {
                        onPlaybackSpeedClick()
                    }
                )
            }
        }
    }
}

@Composable
fun SubtitleSizeDialog(
    currentSize: String,
    onSizeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Subtitle Size",
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            val sizes = listOf(
                "small" to "Small",
                "medium" to "Medium",
                "large" to "Large",
                "extra_large" to "Extra Large"
            )

            items(sizes, key = { it.first }) { (sizeKey, label) ->
                val isSelected = currentSize == sizeKey
                val focusRequester = remember { FocusRequester() }

                SelectionItem(
                    title = label,
                    subtitle = when (sizeKey) {
                        "small" -> "Smaller text"
                        "medium" -> "Default size"
                        "large" -> "Larger text"
                        "extra_large" -> "Maximum size"
                        else -> ""
                    },
                    isSelected = isSelected,
                    onClick = {
                        onSizeChange(sizeKey)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = calculateRoundedValue(8).sdp,
                top = calculateRoundedValue(8).sdp,
                bottom = calculateRoundedValue(4).sdp
            )
    )
}

@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 1.0f, 1.5f, 1.75f, 2.0f, 2.5f, 2.75f, 3.0f)
    SelectionDialog(
        title = "Playback Speed",
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(speeds, key = { it }) { speed ->
                val isSelected = kotlin.math.abs(currentSpeed - speed) < 0.01f
                val label = if (speed == 1.0f) "1x (Default)" else "${speed}x"
                val focusRequester = remember { FocusRequester() }

                SelectionItem(
                    title = label,
                    subtitle = "",
                    isSelected = isSelected,
                    onClick = {
                        onSpeedChange(speed)
                        onDismiss()
                    },
                    focusRequester = if (isSelected) focusRequester else null
                )
            }
        }
    }
}
