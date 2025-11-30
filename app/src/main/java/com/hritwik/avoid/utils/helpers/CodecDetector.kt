package com.hritwik.avoid.utils.helpers

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.Display

object CodecDetector {
    private const val TAG = "CodecDetector"

    
    private var cachedSupportedCodecs: List<String>? = null
    private var cachedSupportedAudioCodecs: List<String>? = null
    private var cachedH264High10Support: Boolean? = null
    private var cachedHevcMain10Support: Boolean? = null
    private var cachedHdrCapabilities: HdrCapabilities? = null
    private var cachedDolbyVisionProfiles: List<String>? = null

    



    private fun isAFTKMDevice(): Boolean {
        val aftkModels = setOf(
            "AFTKM",
            "AFTKRT",
            "AFTMA08C15",
            "AFTMM",
            "AFTKA"
        )
        return aftkModels.contains(Build.MODEL)
    }

    


    data class HdrCapabilities(
        val hdr10: Boolean = false,
        val hdr10Plus: Boolean = false,
        val hlg: Boolean = false,
        val dolbyVision: Boolean = false
    )

    


    data class DeviceCodecInfo(
        val videoCodecs: List<String>,
        val audioCodecs: List<String>,
        val hdrCapabilities: HdrCapabilities,
        val dolbyVisionProfiles: List<String>,
        val hevcMain10: Boolean,
        val avcHigh10: Boolean
    )

    



    fun getSupportedVideoCodecs(): List<String> {
        
        cachedSupportedCodecs?.let { return it }

        val supportedCodecs = mutableSetOf<String>()

        try {
            val codecInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                codecList.codecInfos.toList()
            } else {
                @Suppress("DEPRECATION")
                val count = MediaCodecList.getCodecCount()
                (0 until count).map { index ->
                    @Suppress("DEPRECATION")
                    MediaCodecList.getCodecInfoAt(index)
                }
            }

            for (codecInfo in codecInfos) {
                if (!codecInfo.isEncoder) {
                    val types = codecInfo.supportedTypes
                    for (type in types) {
                        if (type.startsWith("video/")) {
                            supportedCodecs.add(type)
                            Log.d(TAG, "Supported codec: $type (${codecInfo.name})")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting supported codecs", e)
        }

        val result = supportedCodecs.toList()
        cachedSupportedCodecs = result  
        return result
    }

    








    fun isVideoCodecSupported(
        codec: String?,
        videoRange: String? = null,
        videoRangeType: String? = null,
        profile: String? = null,
        bitDepth: Int? = null
    ): Boolean {
        if (codec.isNullOrBlank()) return true 

        val normalizedCodec = codec.lowercase().trim()
        val normalizedProfile = profile?.lowercase()?.trim()

        
        val isHdr = videoRange?.contains("HDR", ignoreCase = true) == true ||
                videoRangeType?.contains("HDR", ignoreCase = true) == true ||
                videoRangeType?.contains("DOVI", ignoreCase = true) == true ||
                videoRangeType?.contains("Dolby", ignoreCase = true) == true

        
        val is10Bit = bitDepth == 10 ||
                      normalizedProfile?.contains("10") == true ||
                      normalizedProfile?.contains("hi10") == true

        
        if (normalizedCodec.contains("h264") || normalizedCodec == "avc") {
            if (is10Bit) {
                
                if (isAFTKMDevice()) {
                    Log.i(TAG, "AFTKM device: H.264 High 10 (10-bit) content will direct play | Profile: $profile, BitDepth: $bitDepth")
                } else {
                    val supportsHigh10 = checkH264High10Support()
                    if (!supportsHigh10) {
                        Log.w(TAG, "H.264 High 10 profile (10-bit / Hi10P) is NOT supported, will need transcoding | Profile: $profile, BitDepth: $bitDepth")
                        return false
                    }
                    Log.d(TAG, "H.264 High 10 profile (10-bit) is supported | Profile: $profile, BitDepth: $bitDepth")
                }
            } else {
                Log.d(TAG, "H.264 8-bit content detected, no High 10 check needed | Profile: $profile, BitDepth: $bitDepth")
            }
        }

        
        if (normalizedCodec.contains("hevc") || normalizedCodec == "h265") {
            if (is10Bit || isHdr) {
                val supportsMain10 = checkHevcMain10Support()
                if (!supportsMain10) {
                    Log.w(TAG, "HEVC Main 10 profile (10-bit) is NOT supported, will need transcoding | Profile: $profile, BitDepth: $bitDepth")
                    return false
                }
                Log.d(TAG, "HEVC Main 10 profile (10-bit) is supported | Profile: $profile, BitDepth: $bitDepth")
            } else {
                Log.d(TAG, "HEVC 8-bit content detected, no Main 10 check needed | Profile: $profile, BitDepth: $bitDepth")
            }
        }

        
        val mimeType = when {
            normalizedCodec.contains("h264") || normalizedCodec == "avc" -> MediaFormat.MIMETYPE_VIDEO_AVC
            normalizedCodec.contains("hevc") || normalizedCodec == "h265" -> MediaFormat.MIMETYPE_VIDEO_HEVC
            normalizedCodec.contains("av1") || normalizedCodec == "av01" -> "video/av01"
            normalizedCodec.contains("vp9") -> MediaFormat.MIMETYPE_VIDEO_VP9
            normalizedCodec.contains("vp8") -> MediaFormat.MIMETYPE_VIDEO_VP8
            normalizedCodec.contains("mpeg4") || normalizedCodec == "mp4v" -> MediaFormat.MIMETYPE_VIDEO_MPEG4
            normalizedCodec.contains("mpeg2") -> "video/mpeg2"
            else -> null
        }

        if (mimeType == null) {
            Log.w(TAG, "Unknown codec: $codec, assuming supported")
            return true
        }

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                val decoderName = codecList.findDecoderForFormat(MediaFormat.createVideoFormat(mimeType, 1920, 1080))

                if (decoderName != null) {
                    Log.d(TAG, "Codec $codec ($mimeType) is supported by decoder: $decoderName")
                    return true
                } else {
                    Log.w(TAG, "Codec $codec ($mimeType) is NOT supported - no decoder found")
                    return false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking codec support for $codec", e)
                
                return getSupportedVideoCodecs().any { it.equals(mimeType, ignoreCase = true) }
            }
        } else {
            
            return getSupportedVideoCodecs().any { it.equals(mimeType, ignoreCase = true) }
        }
    }

    


    fun getCodecDisplayName(codec: String?): String {
        if (codec.isNullOrBlank()) return "Unknown"

        val normalizedCodec = codec.lowercase().trim()
        return when {
            normalizedCodec.contains("h264") || normalizedCodec == "avc" -> "H.264"
            normalizedCodec.contains("hevc") || normalizedCodec == "h265" -> "HEVC (H.265)"
            normalizedCodec.contains("av1") || normalizedCodec == "av01" -> "AV1"
            normalizedCodec.contains("vp9") -> "VP9"
            normalizedCodec.contains("vp8") -> "VP8"
            normalizedCodec.contains("mpeg4") || normalizedCodec == "mp4v" -> "MPEG-4"
            normalizedCodec.contains("mpeg2") -> "MPEG-2"
            else -> codec.uppercase()
        }
    }

    




    fun getSupportedVideoCodecsForJellyfin(): String {
        val mimeTypes = getSupportedVideoCodecs()
        val jellyfinCodecs = mutableListOf<String>()

        
        if (mimeTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }) {
            jellyfinCodecs.add("h264")
        }
        if (mimeTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true) }) {
            jellyfinCodecs.add("hevc")
        }
        if (mimeTypes.any { it.contains("av01", ignoreCase = true) || it.contains("av1", ignoreCase = true) }) {
            jellyfinCodecs.add("av1")
        }
        if (mimeTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_VP9, ignoreCase = true) }) {
            jellyfinCodecs.add("vp9")
        }
        if (mimeTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_VP8, ignoreCase = true) }) {
            jellyfinCodecs.add("vp8")
        }
        if (mimeTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_MPEG4, ignoreCase = true) }) {
            jellyfinCodecs.add("mpeg4")
        }
        if (mimeTypes.any { it.contains("mpeg2", ignoreCase = true) }) {
            jellyfinCodecs.add("mpeg2video")
        }

        val result = jellyfinCodecs.joinToString(",")
        Log.d(TAG, "Supported Jellyfin video codecs: $result")
        return result
    }

    


    fun logSupportedCodecs() {
        val codecs = getSupportedVideoCodecs()
        Log.i(TAG, "Device supports ${codecs.size} video codecs:")
        codecs.forEach { codec ->
            Log.i(TAG, "  - $codec")
        }
    }

    



    fun getSupportedAudioCodecs(): List<String> {
        
        cachedSupportedAudioCodecs?.let { return it }

        val supportedCodecs = mutableSetOf<String>()

        try {
            val codecInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                codecList.codecInfos.toList()
            } else {
                @Suppress("DEPRECATION")
                val count = MediaCodecList.getCodecCount()
                (0 until count).map { index ->
                    @Suppress("DEPRECATION")
                    MediaCodecList.getCodecInfoAt(index)
                }
            }

            for (codecInfo in codecInfos) {
                if (!codecInfo.isEncoder) {
                    val types = codecInfo.supportedTypes
                    for (type in types) {
                        if (type.startsWith("audio/")) {
                            supportedCodecs.add(type)
                            Log.d(TAG, "Supported audio codec: $type (${codecInfo.name})")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting supported audio codecs", e)
        }

        val result = supportedCodecs.toList()
        cachedSupportedAudioCodecs = result
        return result
    }

    


    fun getVideoCodecDisplayNames(): List<String> {
        val mimeTypes = getSupportedVideoCodecs()
        return mimeTypes.map { mimeType ->
            when (mimeType) {
                MediaFormat.MIMETYPE_VIDEO_AVC -> "H.264 (AVC)"
                MediaFormat.MIMETYPE_VIDEO_HEVC -> "H.265 (HEVC)"
                "video/av01" -> "AV1"
                MediaFormat.MIMETYPE_VIDEO_VP9 -> "VP9"
                MediaFormat.MIMETYPE_VIDEO_VP8 -> "VP8"
                MediaFormat.MIMETYPE_VIDEO_MPEG4 -> "MPEG-4"
                "video/mpeg2" -> "MPEG-2"
                MediaFormat.MIMETYPE_VIDEO_H263 -> "H.263"
                MediaFormat.MIMETYPE_VIDEO_MPEG2 -> "MPEG-2"
                MediaFormat.MIMETYPE_VIDEO_RAW -> "RAW"
                MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION -> "Dolby Vision"
                else -> mimeType.removePrefix("video/").uppercase()
            }
        }.distinct().sorted()
    }

    


    fun getAudioCodecDisplayNames(): List<String> {
        val mimeTypes = getSupportedAudioCodecs()
        return mimeTypes.map { mimeType ->
            when (mimeType) {
                MediaFormat.MIMETYPE_AUDIO_AAC -> "AAC"
                MediaFormat.MIMETYPE_AUDIO_AC3 -> "AC3 (Dolby Digital)"
                MediaFormat.MIMETYPE_AUDIO_EAC3 -> "E-AC3 (Dolby Digital Plus)"
                "audio/eac3-joc" -> "Dolby Atmos (E-AC3 JOC)"
                MediaFormat.MIMETYPE_AUDIO_AC4 -> "AC4 (Dolby AC-4)"
                MediaFormat.MIMETYPE_AUDIO_DOLBY_TRUEHD -> "Dolby TrueHD"
                "audio/true-hd" -> "Dolby TrueHD"
                MediaFormat.MIMETYPE_AUDIO_DTS -> "DTS"
                "audio/dts-hd" -> "DTS-HD"
                "audio/dtshd" -> "DTS-HD"
                MediaFormat.MIMETYPE_AUDIO_FLAC -> "FLAC"
                MediaFormat.MIMETYPE_AUDIO_OPUS -> "Opus"
                MediaFormat.MIMETYPE_AUDIO_VORBIS -> "Vorbis"
                MediaFormat.MIMETYPE_AUDIO_MPEG -> "MP3"
                MediaFormat.MIMETYPE_AUDIO_AMR_NB -> "AMR-NB"
                MediaFormat.MIMETYPE_AUDIO_AMR_WB -> "AMR-WB"
                MediaFormat.MIMETYPE_AUDIO_G711_ALAW -> "G.711 A-law"
                MediaFormat.MIMETYPE_AUDIO_G711_MLAW -> "G.711 μ-law"
                MediaFormat.MIMETYPE_AUDIO_RAW -> "PCM"
                MediaFormat.MIMETYPE_AUDIO_MSGSM -> "MS GSM"
                else -> mimeType.removePrefix("audio/").uppercase()
            }
        }.distinct().sorted()
    }

    



    fun getHdrCapabilities(context: Context): HdrCapabilities {
        
        cachedHdrCapabilities?.let { return it }

        var hdr10 = false
        var hdr10Plus = false
        var hlg = false
        var dolbyVision = false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
                val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)

                if (display != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val hdrCaps = display.hdrCapabilities
                    if (hdrCaps != null) {
                        val supportedTypes = hdrCaps.supportedHdrTypes
                        hdr10 = supportedTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10)
                        hlg = supportedTypes.contains(Display.HdrCapabilities.HDR_TYPE_HLG)
                        dolbyVision = supportedTypes.contains(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            hdr10Plus = supportedTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS)
                        }

                        Log.d(TAG, "HDR Capabilities - HDR10: $hdr10, HDR10+: $hdr10Plus, HLG: $hlg, DV: $dolbyVision")
                    }
                }
            }

            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                val codecInfos = codecList.codecInfos

                for (codecInfo in codecInfos) {
                    if (codecInfo.isEncoder) continue

                    val types = codecInfo.supportedTypes

                    
                    if (types.contains(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION)) {
                        dolbyVision = true
                    }

                    
                    if (types.contains(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                        val capabilities = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_HEVC)
                        val profileLevels = capabilities.profileLevels

                        for (profileLevel in profileLevels) {
                            when (profileLevel.profile) {
                                
                                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 -> hdr10 = true
                                
                                MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus -> hdr10Plus = true
                            }
                        }
                    }

                    
                    if (types.contains(MediaFormat.MIMETYPE_VIDEO_VP9)) {
                        val capabilities = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_VP9)
                        val profileLevels = capabilities.profileLevels

                        for (profileLevel in profileLevels) {
                            when (profileLevel.profile) {
                                MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR -> hdr10 = true
                                MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus -> hdr10Plus = true
                                MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR -> hdr10 = true
                                MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR10Plus -> hdr10Plus = true
                            }
                        }
                    }

                    
                    if (types.contains("video/av01")) {
                        val capabilities = codecInfo.getCapabilitiesForType("video/av01")
                        val profileLevels = capabilities.profileLevels

                        for (profileLevel in profileLevels) {
                            when (profileLevel.profile) {
                                MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10 -> hdr10 = true
                                MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus -> hdr10Plus = true
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting HDR capabilities", e)
        }

        val result = HdrCapabilities(
            hdr10 = hdr10,
            hdr10Plus = hdr10Plus,
            hlg = hlg,
            dolbyVision = dolbyVision
        )
        cachedHdrCapabilities = result
        return result
    }

    



    fun getDolbyVisionProfiles(): List<String> {
        
        cachedDolbyVisionProfiles?.let { return it }

        val profiles = mutableListOf<String>()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                val codecInfos = codecList.codecInfos

                for (codecInfo in codecInfos) {
                    if (codecInfo.isEncoder) continue

                    val types = codecInfo.supportedTypes
                    if (!types.contains(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION)) continue

                    val capabilities = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION)
                    val profileLevels = capabilities.profileLevels

                    for (profileLevel in profileLevels) {
                        val profileName = when (profileLevel.profile) {
                            MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDer -> "Profile 4 (dvhe.der)"
                            MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDen -> "Profile 5 (dvhe.den)"
                            MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDtr -> "Profile 7 (dvhe.dtr)"
                            MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheStn -> "Profile 8 (dvhe.stn)"
                            MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt -> "Profile 8.1 (dvhe.st)"
                            MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvavPer -> "Profile 0 (dvav.per)"
                            MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvavPen -> "Profile 1 (dvav.pen)"
                            MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvavSe -> "Profile 9 (dvav.se)"
                            else -> "Profile ${profileLevel.profile}"
                        }
                        if (!profiles.contains(profileName)) {
                            profiles.add(profileName)
                            Log.d(TAG, "Dolby Vision profile supported: $profileName by ${codecInfo.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting Dolby Vision profiles", e)
        }

        val result = profiles.sorted()
        cachedDolbyVisionProfiles = result
        return result
    }

    



    fun getDeviceCodecInfo(context: Context): DeviceCodecInfo {
        return DeviceCodecInfo(
            videoCodecs = getVideoCodecDisplayNames(),
            audioCodecs = getAudioCodecDisplayNames(),
            hdrCapabilities = getHdrCapabilities(context),
            dolbyVisionProfiles = getDolbyVisionProfiles(),
            hevcMain10 = checkHevcMain10Support(),
            avcHigh10 = checkH264High10Support()
        )
    }

    


    fun checkHevcMain10Support(): Boolean {
        
        cachedHevcMain10Support?.let { return it }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            cachedHevcMain10Support = false
            return false
        }

        try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val codecInfos = codecList.codecInfos

            for (codecInfo in codecInfos) {
                if (codecInfo.isEncoder) continue

                val types = codecInfo.supportedTypes
                if (!types.contains(MediaFormat.MIMETYPE_VIDEO_HEVC)) continue

                val capabilities = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_HEVC)
                val profileLevels = capabilities.profileLevels

                for (profileLevel in profileLevels) {
                    
                    if (profileLevel.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10) {
                        Log.d(TAG, "HEVC Main 10 profile IS supported by ${codecInfo.name}")
                        cachedHevcMain10Support = true
                        return true
                    }
                }
            }

            Log.w(TAG, "HEVC Main 10 profile is NOT supported on this device")
            cachedHevcMain10Support = false
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking HEVC Main 10 support", e)
            cachedHevcMain10Support = false
            return false
        }
    }

    


    fun checkH264High10Support(): Boolean {
        
        cachedH264High10Support?.let { return it }

        
        if (isAFTKMDevice()) {
            Log.i(TAG, "AFTKM device detected - enabling H.264 High 10 (Hi10P) support override")
            cachedH264High10Support = true
            return true
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            cachedH264High10Support = false
            return false
        }

        try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val codecInfos = codecList.codecInfos

            for (codecInfo in codecInfos) {
                if (codecInfo.isEncoder) continue

                val types = codecInfo.supportedTypes
                if (!types.contains(MediaFormat.MIMETYPE_VIDEO_AVC)) continue

                val capabilities = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                val profileLevels = capabilities.profileLevels

                for (profileLevel in profileLevels) {
                    
                    if (profileLevel.profile == MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10) {
                        Log.d(TAG, "H.264 High 10 profile IS supported by ${codecInfo.name}")
                        cachedH264High10Support = true
                        return true
                    }
                }
            }

            Log.w(TAG, "H.264 High 10 profile is NOT supported on this device")
            cachedH264High10Support = false
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking H.264 High 10 support", e)
            cachedH264High10Support = false
            return false
        }
    }

    


    fun isAudioCodecSupported(codec: String): Boolean {
        val normalizedCodec = codec.lowercase().trim()
        val mimeType = when {
            normalizedCodec.contains("aac") -> MediaFormat.MIMETYPE_AUDIO_AAC
            normalizedCodec.contains("ac3") && !normalizedCodec.contains("eac3") -> MediaFormat.MIMETYPE_AUDIO_AC3
            normalizedCodec.contains("eac3") || normalizedCodec.contains("e-ac3") -> MediaFormat.MIMETYPE_AUDIO_EAC3
            normalizedCodec.contains("truehd") -> MediaFormat.MIMETYPE_AUDIO_DOLBY_TRUEHD
            normalizedCodec.contains("dts") -> MediaFormat.MIMETYPE_AUDIO_DTS
            normalizedCodec.contains("flac") -> MediaFormat.MIMETYPE_AUDIO_FLAC
            normalizedCodec.contains("opus") -> MediaFormat.MIMETYPE_AUDIO_OPUS
            normalizedCodec.contains("vorbis") -> MediaFormat.MIMETYPE_AUDIO_VORBIS
            normalizedCodec.contains("mp3") || normalizedCodec.contains("mpeg") -> MediaFormat.MIMETYPE_AUDIO_MPEG
            else -> return false
        }

        return getSupportedAudioCodecs().any { it.equals(mimeType, ignoreCase = true) }
    }
}
