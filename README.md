# 
</div>

<div align="center">

<img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/alpha-v-0.2.1/Fugaz%20One_void.png" alt="Void Logo" width="160" height="160">

**A modern, minimal Android client for Jellyfin**

Built entirely with **Kotlin** and **Jetpack Compose**

[![Android API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5.15-green.svg)](https://developer.android.com/jetpack/compose)

</div>

---

## ✨ Features

### 🎯 Core Functionality
- **Jellyfin Integration** - Full server authentication and user management
- **Library Browsing** - Explore your media collection with intuitive navigation
- **Advanced Search** - Find your content quickly with comprehensive search
- **Detailed Media Views** - Rich metadata display with cast, crew, and plot information

### 🎵 Media Playback
- **MPV Player** - High-quality video playback with excellent format support
- **ExoPlayer Support** - Alternative playback engine for broader compatibility
- **Theme Song Support** - Immersive experience with background audio

### 📱 Modern UI/UX
- **Material 3 Design** - Beautiful, adaptive UI following Google's design principles
- **Dynamic Themes** - Colors that adapt to your content and system preferences
- **Ambient Backgrounds** - Stunning visual effects that enhance your viewing experience
- **Responsive Layout** - Optimized for phones, tablets, and various screen sizes

### 🔄 Advanced Features
- **Multiple Quality Options** - Choose from Auto, 4K, 1080p, 720p, 480p, 360p
- **Subtitle Support** - Full subtitle support with customizable sizing

---

## 📱 Screenshots

<div align="center">

| Home | Library | Library (Grid) | Movie |
|------|---------|----------------|-------|
| <img src="./VoidTV-screenshots/VoidTV_Home.png" alt="Home" width="1280" height="720"> | <img src="./VoidTV-screenshots/VoidTV_Library.png" alt="Library" width="1280" height="720"> | <img src="./VoidTV-screenshots/VoidTV_library%20%282%29.png" alt="Library Grid" width="1280" height="720"> | <img src="./VoidTV-screenshots/VoidTV_Movie.png" alt="Movie" width="1280" height="720"> |

| Search | Profile | Season | Movie Navigation |
|--------|---------|--------|------------------|
| <img src="./VoidTV-screenshots/VoidTV_Search.png" alt="Search" width="1280" height="720"> | <img src="./VoidTV-screenshots/VoidTV_Profile.png" alt="Profile" width="1280" height="720"> | <img src="./VoidTV-screenshots/VoidTV_season.png" alt="Season" width="1280" height="720"> | <img src="./VoidTV-screenshots/movie_nav.png" alt="Movie Navigation" width="1280" height="720"> |
</div>


## 🛠️ Tech Stack & Architecture

### Platform & Framework
- **Target SDK**: 35 (Android 14)
- **Minimum SDK**: 26 (Android 8.0)
- **Language**: Kotlin 1.9.0
- **UI Framework**: Jetpack Compose 1.5.15

### Core Libraries
- **Architecture**: Clean Architecture (Data/Domain/Presentation layers)
- **Dependency Injection**: Hilt
- **Async Programming**: Kotlin Coroutines + Flow
- **Navigation**: Navigation Compose
- **State Management**: ViewModel + StateFlow

### Media & Networking
- **Media Player**: MPV-Android + ExoPlayer (Media3)
- **Image Loading**: Coil (with GIF/SVG support)
- **Networking**: Retrofit + OkHttp
- **Serialization**: Kotlinx Serialization
- **Jellyfin SDK**: Jellyfin Core

### Storage & Persistence
- **Database**: Room
- **Preferences**: DataStore
- **Work Manager**: Background tasks and downloads
- **File Provider**: Secure file sharing

### UI Components
- **Design System**: Material 3
- **Icons**: Material Icons Extended
- **Responsive Design**: SDP/SSP Compose
- **Color Extraction**: Palette API
- **Permissions**: Accompanist Permissions

---

## 🔧 Configuration

### Player Settings
- **Primary Player**: MPV (recommended for best performance)
- **Fallback Player**: ExoPlayer (better compatibility)
- **Display Mode**: Fit Screen, Fill Screen, Original Size
- **Hardware Acceleration**: Enabled by default

### Download Settings
- **Quality Options**: Auto, 4K, 1080p, 720p, 480p, 360p
- **Storage Location**: Internal/External storage
- **Auto-cleanup**: Automatic removal of old downloads

### Streaming Settings
- **Adaptive Bitrate**: Automatic quality adjustment
- **Buffer Size**: Configurable for different network conditions
- **Direct Play**: When supported by server and device

---

## 📄 License

```
Copyright (c) 2025 Hritwik Johri

All rights reserved.

This software and its source code are proprietary and confidential.  
Unauthorized copying, modification, distribution, or use of this software,  
in whole or in part, is strictly prohibited without prior written permission  
from the copyright holder.

The software is provided "AS IS," without warranty of any kind, express or implied,  
including but not limited to the warranties of merchantability, fitness for a  
particular purpose, and noninfringement. In no event shall the authors or  
copyright holders be liable for any claim, damages, or other liability, whether  
in an action of contract, tort, or otherwise, arising from, out of, or in  
connection with the software or the use or other dealings in the software.

For licensing inquiries, please contact: hritwikjohri@gmail.com
```

---

## 🙏 Acknowledgments

### Special Thanks
- **[Jellyfin Project](https://jellyfin.org/)** - For creating the amazing open-source media server
- **[Findroid](https://github.com/jarnedemeulemeester/findroid) & [Streamyfin](https://github.com/frederic-loui/streamyfin) developers** - For inspiration and feature references
- **[MPV](https://mpv.io/) & AndroidX Media3 teams** - For excellent media playback capabilities
- **[@nitanmarcel](https://github.com/nitanmarcel)** - For the mpv-compose library that powers our video playback

### Built With Love Using
- **Jetpack Compose** - Modern Android UI toolkit
- **Material You** - Google's design system
- **Kotlin Coroutines** - Asynchronous programming
- **Hilt** - Dependency injection
- **Coil** - Image loading library

---

## ☕ Support the Project



<div align="center">
If you find Void useful and want to support its development, consider buying me a coffee!


[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-yellow?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/hritwikjohri)

**Your support helps maintain and improve Void for everyone! 🚀**

---

## 🤝 Collaborators

<div align="center">

| [Hritwik Johri](https://github.com/hritwikjohri) | [KHazard](https://github.com/khazard) |
|:---:|:---:|
| Lead Developer | Contributor |

</div>

---

<div align="center">

**Made with ❤️ by the Void Team**

[⭐ Star this repository](../../stargazers) | [🐛 Report Bug](../../issues) | [💡 Request Feature](../../issues)

</div>
