package com.example.myapplication // CHECK: Does this match your package name?


import android.graphics.Color as AndroidColor
import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.Image as GlanceImage
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
class VinylWidget : GlanceAppWidget() {

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        provideContent {
            val songTitle by MusicData.songTitle.collectAsState()
            val albumArt by MusicData.albumArt.collectAsState()

            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // THE MAIN STACK: Disk -> Art -> Hole
                Box(
                    modifier = GlanceModifier
                        .size(170.dp)
                        .clickable(actionRunCallback<ToggleMusicCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    // 1. THE BLACK VINYL DISK
                    GlanceImage(
                        provider = ImageProvider(R.drawable.ic_vinyl_disk),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxSize()
                    )

                    // 2. THE ALBUM ART (Center Label)
                    val art: Bitmap? = albumArt
                    if (art != null) {
                        GlanceImage(
                            provider = ImageProvider(art),
                            contentDescription = "Art",
                            modifier = GlanceModifier
                                .size(75.dp)
                                .cornerRadius(37.5.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // 3. THE CENTER HOLE
                    // Placing this last ensures it stays on top of the album art
                    Box(
                        modifier = GlanceModifier
                            .size(6.dp)
                            .cornerRadius(3.dp)
                            .background(ColorProvider(androidx.compose.ui.graphics.Color.Black))
                    ) {}
                }

                Spacer(modifier = GlanceModifier.height(12.dp))

                // SONG TITLE
                Text(
                    text = songTitle,
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text),
                        fontSize = 16.sp,
                        fontWeight = androidx.glance.text.FontWeight.Bold,
                        textAlign = androidx.glance.text.TextAlign.Center
                    )
                )
            }
        }
    }
}

// THE CLICK HANDLER: Put this right below the VinylWidget class


class ToggleMusicCallback : ActionCallback {
    override suspend fun onAction(
        context: android.content.Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Calls the companion function we fixed in your Service!
        MusicNotificationListener.togglePlayback(context)
    }
}