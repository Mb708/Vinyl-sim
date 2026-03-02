# VinylSpin: Retro Music Visualizer
A lightweight Android application that brings the nostalgic feel of vinyl records to your digital music library. VinylSpin tracks your active music playback in real-time, extracts album art, and displays it on a smooth, animated virtual turntable.

## 🎵 Core Features
Live Metadata Tracking: Seamlessly connects to major music streaming services (Spotify, YouTube Music, Apple Music) to pull song titles, artist names, and artwork.

Dynamic Visualizer: Uses a high-performance rendering loop (powered by Jetpack Compose) to simulate the rotation and aesthetic of a physical record.

Real-time Synchronization: The UI updates instantly as you switch tracks or change playback state.

Media Control Integration: Built-in MediaController support ensures the app reacts correctly to play, pause, and seek commands.

## 🚀 How it Works
The app operates using a notification listener. It bypasses the need for complex APIs by tapping into the MediaSession tokens provided by the Android system.

Notification Listener: Monitors active music notifications.

Metadata Extraction: Parses the MediaSession.Token to retrieve song data.

UI Bridge: Maps artwork to a responsive Bitmap and passes it to the Compose rendering engine for display.

## 🛠 Tech Stack
Language: Kotlin

UI Toolkit: Jetpack Compose

Data Handling: MediaSession APIs and NotificationListenerService

Graphics: Native Canvas and Bitmap processing

## Installation 
You can download the latest version of the app directly from our releases page: 
Note: Since this is a self-distributed build, you may need to enable "Install from unknown sources" in your Android security settings to install the APK.
