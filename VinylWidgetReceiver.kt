package com.example.myapplication

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class VinylWidgetReceiver : GlanceAppWidgetReceiver() {

    // 1. The error says it wants 'glanceAppWidget', so we give it exactly that:
    override val glanceAppWidget: VinylWidget = VinylWidget()

    // 2. We use the Coroutine Scope to update the widget when it's first enabled
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MainScope().launch {
            // Use the variable we defined above
            glanceAppWidget.updateAll(context)
        }
    }
}