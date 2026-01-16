plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "com.hritwik.avoid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hritwik.avoid"
        minSdk = 26
        targetSdk = 36
        versionCode = 25
        versionName = "Alpha-0.2.8.1tv"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        
        jniLibs {
            pickFirsts += setOf(
                
                "lib/arm64-v8a/libavcodec.so",
                "lib/arm64-v8a/libavdevice.so",
                "lib/arm64-v8a/libavfilter.so",
                "lib/arm64-v8a/libavformat.so",
                "lib/arm64-v8a/libavutil.so",
                "lib/arm64-v8a/libswresample.so",
                "lib/arm64-v8a/libswscale.so",
                "lib/arm64-v8a/libc++_shared.so",
                
                "lib/armeabi-v7a/libavcodec.so",
                "lib/armeabi-v7a/libavdevice.so",
                "lib/armeabi-v7a/libavfilter.so",
                "lib/armeabi-v7a/libavformat.so",
                "lib/armeabi-v7a/libavutil.so",
                "lib/armeabi-v7a/libswresample.so",
                "lib/armeabi-v7a/libswscale.so",
                "lib/armeabi-v7a/libc++_shared.so",
                
                "lib/x86/libavcodec.so",
                "lib/x86/libavdevice.so",
                "lib/x86/libavfilter.so",
                "lib/x86/libavformat.so",
                "lib/x86/libavutil.so",
                "lib/x86/libswresample.so",
                "lib/x86/libswscale.so",
                "lib/x86/libc++_shared.so",
                
                "lib/x86_64/libavcodec.so",
                "lib/x86_64/libavdevice.so",
                "lib/x86_64/libavfilter.so",
                "lib/x86_64/libavformat.so",
                "lib/x86_64/libavutil.so",
                "lib/x86_64/libswresample.so",
                "lib/x86_64/libswscale.so",
                "lib/x86_64/libc++_shared.so"
            )
        }
    }

    lint {
        disable.add("NullSafeMutableLiveData")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

configurations.configureEach {
    exclude(group = "androidx.media3", module = "media3-exoplayer")
    exclude(group = "androidx.media3", module = "media3-extractor")
}

dependencies {
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    
    implementation(libs.androidx.tv.foundation)

    
    implementation(libs.sdp.compose)
    implementation(libs.ssp.android)

    
    implementation(libs.androidx.navigation.compose)

    
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)

    
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    
    implementation(libs.jellyfin.core)

    
    implementation(files("libs/mpv-compose.aar"))

    
    implementation(files("libs/lib-exoplayer-release.aar"))
    implementation(files("libs/lib-extractor-release.aar"))
    implementation(files("libs/lib-decoder-ffmpeg-release.aar"))
    implementation(files("libs/lib-exoplayer-hls-release.aar"))
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.smoothstreaming)
    implementation(libs.androidx.media3.exoplayer.rtsp)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.decoder)
    implementation(libs.androidx.media3.cast)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.test.utils)

    
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.converter.gson)
    implementation(libs.androidx.work.runtime.ktx)
    
    
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.animation.graphics)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.databinding.adapters)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.coil)
    implementation("com.github.woltapp:blurhash:master-SNAPSHOT")
    implementation(libs.zxing.core)
    implementation(libs.nanohttpd)
    implementation(libs.tink)
    implementation(libs.conscrypt.android)
    implementation(libs.bouncycastle)
    ksp(libs.androidx.room.compiler)

    
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    
    
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
