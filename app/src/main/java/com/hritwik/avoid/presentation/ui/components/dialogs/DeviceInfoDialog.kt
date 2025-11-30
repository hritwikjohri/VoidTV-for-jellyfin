package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.hritwik.avoid.utils.helpers.CodecDetector
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import com.hritwik.avoid.utils.helpers.getDeviceName
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceInfoDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val deviceInfo = remember { CodecDetector.getDeviceCodecInfo(context) }
    val deviceName = remember { getDeviceName(context) }

    VoidAlertDialog(
        visible = true,
        title = "Device Information",
        icon = Icons.Default.Info,
        onDismissRequest = onDismiss,
        dismissText = "Close",
        onDismissButton = onDismiss,
        content = {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(calculateRoundedValue(12).sdp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = calculateRoundedValue(600).sdp),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
            ) {
                item {
                    InfoSection(title = "HDR Capabilities") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                        ) {
                            HdrCapabilityRow("HDR10", deviceInfo.hdrCapabilities.hdr10)
                            HdrCapabilityRow("HDR10+", deviceInfo.hdrCapabilities.hdr10Plus)
                            HdrCapabilityRow("HLG", deviceInfo.hdrCapabilities.hlg)
                            HdrCapabilityRow("Dolby Vision", deviceInfo.hdrCapabilities.dolbyVision)
                        }
                    }
                }

                item {
                    InfoSection(title = "10-bit Profiles") {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                        ) {
                            HdrCapabilityRow("HEVC Main 10", deviceInfo.hevcMain10)
                            HdrCapabilityRow("AVC High 10", deviceInfo.avcHigh10)
                        }
                    }
                }

                if (deviceInfo.dolbyVisionProfiles.isNotEmpty()) {
                    item {
                        InfoSection(title = "Dolby Vision Profiles") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
                                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                            ) {
                                deviceInfo.dolbyVisionProfiles.forEach { profile ->
                                    CodecChip(text = profile)
                                }
                            }
                        }
                    }
                }

                item {
                    InfoSection(title = "Video Codecs") {
                        if (deviceInfo.videoCodecs.isEmpty()) {
                            Text(
                                text = "No video codecs detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
                                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                            ) {
                                deviceInfo.videoCodecs.forEach { codec ->
                                    CodecChip(text = codec)
                                }
                            }
                        }
                    }
                }

                item {
                    InfoSection(title = "Audio Codecs") {
                        if (deviceInfo.audioCodecs.isEmpty()) {
                            Text(
                                text = "No audio codecs detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
                                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                            ) {
                                deviceInfo.audioCodecs.forEach { codec ->
                                    CodecChip(text = codec)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))
                }
            }
        }
    )
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))
        content()
        Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun HdrCapabilityRow(
    name: String,
    supported: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            Icon(
                imageVector = if (supported) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (supported) "Supported" else "Not Supported",
                modifier = Modifier.size(calculateRoundedValue(16).sdp),
                tint = if (supported) Color(0xFF4CAF50) else Color(0xFFE57373)
            )
            Text(
                text = if (supported) "Supported" else "Not Supported",
                style = MaterialTheme.typography.bodySmall,
                color = if (supported) Color(0xFF4CAF50) else Color(0xFFE57373)
            )
        }
    }
}

@Composable
private fun CodecChip(
    text: String
) {
    SuggestionChip(
        onClick = { },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = BorderStroke(
            width = calculateRoundedValue(1).sdp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    )
}
