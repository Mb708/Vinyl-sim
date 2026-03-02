package com.example.myapplication

import android.R.attr.textSize
import android.service.wallpaper.WallpaperService
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.graphics.Paint
import android.graphics.Color

class VinylWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VinylEngine()

    inner class VinylEngine : Engine() {
        private var rotation = 0f
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = object : Runnable {
            override fun run() {
                draw()
            }
        }

        fun draw() {
            val canvas = surfaceHolder.lockCanvas() ?: return
            try {
                // 1. Fill background so it's not "stuck" on the last frame
                canvas.drawColor(Color.DKGRAY)

                val bitmap = ImageBridge.currentBitmap
                if (bitmap != null) {
                    val centerX = canvas.width / 2f
                    val centerY = canvas.height / 2f

                    val matrix = Matrix()
                    // Center the bitmap
                    matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
                    // Rotate it
                    matrix.postRotate(rotation, 0f, 0f)
                    // Move to center of screen
                    matrix.postTranslate(centerX, centerY)

                    canvas.drawBitmap(bitmap, matrix, null)
                } else {
                    // DRAW A TEST CIRCLE so you know the wallpaper is alive
                    val paint = Paint().apply { color = Color.WHITE; textSize = 40f }
                    canvas.drawText("Waiting for music...", 100f, 100f, paint)
                    canvas.drawCircle(canvas.width / 2f, canvas.height / 2f, 100f, paint)
                }
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) draw() else handler.removeCallbacks(drawRunnable)
        }
    }
}