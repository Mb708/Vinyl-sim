package com.example.myapplication

import android.graphics.Bitmap

    object ImageBridge {
        var currentBitmap: Bitmap? = null
        var onImageChanged: ((Bitmap) -> Unit)? = null

        fun updateImage(newBitmap: Bitmap) {
            currentBitmap = newBitmap
            onImageChanged?.invoke(newBitmap)
        }
    }
