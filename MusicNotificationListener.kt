package com.example.myapplication

import android.annotation.SuppressLint
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.glance.appwidget.updateAll // THE CRITICAL IMPORT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.app.Notification
import android.app.Service.START_STICKY
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import androidx.palette.graphics.Palette
import androidx.core.os.BundleCompat
import android.media.session.MediaSession
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.media.MediaMetadata
import androidx.core.graphics.drawable.toBitmap
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.cancel // This fixes the 'cancel' error
import kotlinx.coroutines.SupervisorJob
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.launch
import android.graphics.Canvas

class MusicNotificationListener : NotificationListenerService() {

    // 1. Define a scope to handle "suspend" functions like the widget update
    // This adds a Job to a Dispatcher, which is the correct way to build a Context
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        var activeController: MediaController? = null

        // Helper to ensure we have a controller before doing anything
        private fun ensureController(context: Context): MediaController? {
            if (activeController == null) {
                val mm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
                val component = ComponentName(context, MusicNotificationListener::class.java)
                val sessions = mm.getActiveSessions(component)
                if (sessions.isNotEmpty()) {
                    activeController = sessions[0]
                }
            }
            return activeController
        }

        fun togglePlayback(context: Context) {
            println("!!! NEEDLE: Toggle Attempted !!!")
            ensureController(context)?.let { controller ->
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }
        }

        fun skipNext(context: Context) {
            println("!!! SWIPE: Skip Next !!!")
            ensureController(context)?.transportControls?.skipToNext()
        }

        fun skipPrevious(context: Context) {
            println("!!! SWIPE: Smart Previous !!!")
            ensureController(context)?.let { controller ->
                val playbackState = controller.playbackState
                val currentPosition = playbackState?.position ?: 0L

                // If we are more than 3 seconds (3000ms) into the song, just restart it
                if (currentPosition > 3000) {
                    controller.transportControls.seekTo(0)
                } else {
                    // If we are at the beginning, go to the actual previous track
                    controller.transportControls.skipToPrevious()
                }
            }
        }
    }

    @SuppressLint("UseKtx")
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val packageName = sbn.packageName

        // 1. SAFE IMAGE EXTRACTION (No casting crashes here)
        val largeIcon = extras.get(Notification.EXTRA_LARGE_ICON)

        // We create a variable to hold the result so we don't have to repeat code
        var finalBitmap: Bitmap? = null

        if (largeIcon is Bitmap) {
            finalBitmap = largeIcon
        } else if (largeIcon is android.graphics.drawable.Icon) {
            val drawable = largeIcon.loadDrawable(this)
            if (drawable is android.graphics.drawable.BitmapDrawable) {
                finalBitmap = drawable.bitmap
            } else {
                val bitmap = Bitmap.createBitmap(
                    drawable?.intrinsicWidth?.coerceAtLeast(1) ?: 1,
                    drawable?.intrinsicHeight?.coerceAtLeast(1) ?: 1,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable?.setBounds(0, 0, canvas.width, canvas.height)
                drawable?.draw(canvas)
                finalBitmap = bitmap
            }
        }

        // 2. UPDATE THE BRIDGE (For Wallpaper)
        finalBitmap?.let {
            ImageBridge.updateImage(it)
        }

        // 3. MEDIA SESSION LOGIC
        val mediaToken = if (android.os.Build.VERSION.SDK_INT >= 33) {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
        }

        if (mediaToken != null) {
            activeController = MediaController(this, mediaToken)
            val state = activeController?.playbackState?.state
            MusicData.isPlaying.value = (state == PlaybackState.STATE_PLAYING)

            // Filter for specific apps
            val allowedApps = listOf(
                "com.spotify.music", // Fixed the spotify package name
                "com.google.android.apps.youtube.music",
                "com.apple.android.music"
            )

            if (allowedApps.contains(packageName)) {
                val metadata = activeController?.metadata
                if (metadata != null) {
                    MusicData.songTitle.value = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
                    MusicData.artistName.value = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown"

                    // Check metadata for high-res art if available
                    val metaArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

                    if (metaArt != null) {
                        updateUIWithBitmap(metaArt)
                        ImageBridge.updateImage(metaArt) // Keep wallpaper in sync
                    } else if (finalBitmap != null) {
                        updateUIWithBitmap(finalBitmap)
                    } else {
                        handleIconFallback(sbn.notification)
                    }
                }
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Run widget update inside our scope to satisfy 'suspend'
        serviceScope.launch {
            try { VinylWidget().updateAll(this@MusicNotificationListener) } catch (e: Exception) {}
        }

        activeNotifications?.forEach { sbn ->
            onNotificationPosted(sbn)
        }
    }

    private fun updateUIWithBitmap(bitmap: Bitmap) {
        MusicData.albumArt.value = bitmap

        // Handle background color
        androidx.palette.graphics.Palette.from(bitmap).generate { palette ->
            val color = palette?.getDarkVibrantColor(0xFF333333.toInt()) ?: 0xFF333333.toInt()
            MusicData.backgroundColor.value = color
        }

        // Update the Widget inside the Coroutine Scope
        serviceScope.launch {
            try {
                VinylWidget().updateAll(this@MusicNotificationListener)
                println("!!! WIDGET: Update Sent !!!")
            } catch (e: Exception) {
                println("!!! WIDGET: Update Failed: ${e.message}")
            }
        }
    }

    @SuppressLint("UseKtx")
    private fun handleIconFallback(notification: Notification) {
        notification.getLargeIcon()?.loadDrawable(this)?.let { drawable ->
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(512),
                drawable.intrinsicHeight.coerceAtLeast(512),
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            updateUIWithBitmap(bitmap)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.spotify.music" || sbn.packageName == "com.google.android.apps.youtube.music") {
            MusicData.isPlaying.value = false
            MusicData.songTitle.value = "No Music Playing"

            serviceScope.launch {
                try { VinylWidget().updateAll(this@MusicNotificationListener) } catch (e: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Clean up to prevent memory leaks
    }
}

