package com.example.myapplication

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow


object MusicData {
    val songTitle = MutableStateFlow("No Music Playing")
    val albumArt = MutableStateFlow<Bitmap?>(null)
    val backgroundColor = MutableStateFlow(0xFF333333.toInt())

    val artistName = MutableStateFlow("")
    val isPlaying = MutableStateFlow(false)
}