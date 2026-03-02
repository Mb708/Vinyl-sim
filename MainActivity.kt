package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.offset
import android.app.WallpaperManager
import android.content.ComponentName
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val currentSong by MusicData.songTitle.collectAsState()
    val currentArtist by MusicData.artistName.collectAsState()
    val bgColorInt by MusicData.backgroundColor.collectAsState()
    val albumArt by MusicData.albumArt.collectAsState()
    val isPlaying by MusicData.isPlaying.collectAsState()
    val context = LocalContext.current
    val dragOffset = remember { Animatable(0f) }
    val animatedBgColor by animateColorAsState(
        targetValue = Color(bgColorInt),
        animationSpec = tween(durationMillis = 1000),
        label = "BgColorAnimation"
    )

    // 1. OUTERMOST BOX (The Background Layer)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
            .background(Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent, Color.Black.copy(alpha = 0.4f))
            )),
        contentAlignment = Alignment.Center
    ) {
        // 2. CONTENT COLUMN (The UI Layer)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (currentSong != "No Music Playing") {

                // ONLY ONE NEEDLE HERE - Placed correctly in the layout
                VinylWithNeedle(
                    isPlaying = isPlaying,
                    art = albumArt,
                    onTogglePlay = {
                        // This version talks to the Service AND shows the Toast
                        android.widget.Toast.makeText(context, "Needle Tapped!", android.widget.Toast.LENGTH_SHORT).show()
                        MusicNotificationListener.togglePlayback(context)
                    }
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = currentSong,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentArtist,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

            } else {
                PermissionScreen()
            }
        }
    }
}

@Composable
fun PermissionScreen() {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Vinyl is ready",
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
        }) {
            Text("Allow Vinyl to see Music")
        }
    }
}

@Composable
fun VinylWithNeedle(isPlaying: Boolean, art: android.graphics.Bitmap?, onTogglePlay: () -> Unit) {
    val needleAngle by animateFloatAsState(
        targetValue = if (isPlaying) 0f else -30f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "NeedleRotation"
    )

    val swipeThreshold = 50f

    // THE MASTER BOX - This must be the ONLY thing with .clickable
    Box(
        modifier = Modifier
            .size(400.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onTogglePlay()
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. THE RECORD
        // REMOVE any .clickable modifiers from inside VinylRecord!
        VinylRecord(isPlaying = isPlaying, art = art)

        // 2. THE NEEDLE
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = needleAngle
                    transformOrigin = TransformOrigin(0.85f, 0.15f)
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pivotX = size.width * 0.85f
                val pivotY = size.height * 0.15f

                drawCircle(color = Color(0xFF444444), radius = 25f, center = Offset(pivotX, pivotY))
                drawLine(
                    color = Color(0xFFCCCCCC),
                    start = Offset(pivotX, pivotY),
                    end = Offset(pivotX - 30f, pivotY + 200f),
                    strokeWidth = 12f,
                    cap = StrokeCap.Round
                )
                drawRoundRect(
                    color = Color(0xFF222222),
                    topLeft = Offset(pivotX - 55f, pivotY + 190f),
                    size = Size(50f, 70f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }
    }
}
@Composable
fun VinylRecord(isPlaying: Boolean, art: android.graphics.Bitmap?) {
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope() // Fixes 'Unresolved reference rememberCoroutineScope'
    var totalDragBy by remember { mutableStateOf(0f) } // Fixes 'Unresolved reference totalDragBy'
    var hasTriggeredSwipe by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val dragOffset = remember { Animatable(0f) }


    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // Animates continuously
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(10000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            rotation.stop()
        }
    }

    Box(
        modifier = Modifier
            .size(300.dp)
            .offset(x = dragOffset.value.dp)
            // 1. Gesture detection should happen on the base coordinates
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDragBy = 0f  // CRITICAL: Reset the counter!
                        hasTriggeredSwipe = false
                    },
                    onDragCancel = {
                        totalDragBy = 0f
                        hasTriggeredSwipe = false
                    },
                    onDragEnd = {
                        totalDragBy = 0f
                        hasTriggeredSwipe = false
                        scope.launch {
                            dragOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragBy += dragAmount

                        scope.launch {
                            dragOffset.snapTo(totalDragBy * 0.3f)
                        }


                        if (!hasTriggeredSwipe) {
                            if (totalDragBy < -100f || totalDragBy > 100f) {
                                hasTriggeredSwipe = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                scope.launch {
                                    // Pop slightly more to 1.2f so it's clearly visible
                                    scale.animateTo(1.2f, spring(Spring.DampingRatioLowBouncy))
                                    scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                                }

                                if (totalDragBy < 0) MusicNotificationListener.skipNext(context)
                                else MusicNotificationListener.skipPrevious(context)
                            }
                        }
                    },

                )
            }
            // 2. Apply animations LAST so they don't move the touch area
            .scale(scale.value)
            .rotate(rotation.value % 360f)
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ){
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                filterQuality = FilterQuality.High,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray))
        }

        // Grooves and Gloss layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = 170f,
                sweepAngle = 50f,
                useCenter = true
            )
        }
    }
}

@Composable
fun LockScreenControlCenter() {
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Your existing spinning record code here...

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, VinylWallpaperService::class.java)
                )
            }
            context.startActivity(intent)
        }) {
            Text("Apply to Lock Screen")
        }
    }
}