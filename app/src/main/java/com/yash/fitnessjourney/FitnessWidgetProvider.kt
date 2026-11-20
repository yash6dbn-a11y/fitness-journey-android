package com.yash.fitnessjourney

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Locale

class FitnessWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FitnessWidgetProvider::class.java))
            ids.forEach { updateOne(context, manager, it) }
        }
        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_fitness_progress)
            val current = FitnessData.current(context)
            views.setTextViewText(R.id.current_weight, String.format(Locale.getDefault(), "%.2f kg", current))
            views.setTextViewText(R.id.lost_weight, String.format(Locale.getDefault(), "↓ %.2f kg", FitnessData.lost(context)))
            views.setTextViewText(R.id.left_weight, String.format(Locale.getDefault(), "%.2f kg", FitnessData.remaining(context)))
            views.setTextViewText(R.id.start_label, String.format(Locale.getDefault(), "%.1f kg start", FitnessData.start(context)))
            views.setTextViewText(R.id.target_label, String.format(Locale.getDefault(), "%.0f kg target", FitnessData.target(context)))
            views.setProgressBar(R.id.progress_bar, 100, FitnessData.progress(context).toInt(), false)
            val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, intent)
            manager.updateAppWidget(id, views)
        }
    }
}
