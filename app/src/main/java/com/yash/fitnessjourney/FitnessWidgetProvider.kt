package com.yash.fitnessjourney

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.widget.RemoteViews
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

class FitnessWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateOne(context, appWidgetManager, appWidgetId)
    }

    companion object {
        private const val ACCENT = "#61C70B"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FitnessWidgetProvider::class.java))
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_fitness_progress)
            val heightDp = cardHeightDp(context, manager.getAppWidgetOptions(id))
            val gapDp = 7.5f
            val diameterDp = (heightDp - gapDp * 2f).coerceAtLeast(32f)
            if (Build.VERSION.SDK_INT >= 31) {
                val density = context.resources.displayMetrics.density
                views.setViewPadding(
                    R.id.widget_root,
                    (gapDp * density).toInt(),
                    (gapDp * density).toInt(),
                    (14f * density).toInt(),
                    (gapDp * density).toInt()
                )
                views.setViewLayoutWidth(R.id.progress_ring, diameterDp, TypedValue.COMPLEX_UNIT_DIP)
                views.setViewLayoutHeight(R.id.progress_ring, diameterDp, TypedValue.COMPLEX_UNIT_DIP)
            }
            views.setTextViewText(R.id.current_weight, formatKg(FitnessData.current(context)))
            views.setTextViewText(R.id.lost_weight, formatKg(FitnessData.lost(context)))
            views.setTextViewText(R.id.remaining_weight, formatKg(FitnessData.remaining(context)))
            views.setImageViewBitmap(R.id.progress_ring, progressRing(context, FitnessData.progress(context), diameterDp))
            val intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, intent)
            manager.updateAppWidget(id, views)
        }

        private fun formatKg(value: Float): String {
            val tenths = round(value * 10f) / 10f
            val whole = round(tenths)
            return if (abs(tenths - whole) < 0.05f) {
                String.format(Locale.getDefault(), "%.0f", whole)
            } else {
                String.format(Locale.getDefault(), "%.1f", tenths)
            }
        }

        private fun cardHeightDp(context: Context, options: Bundle): Float {
            if (Build.VERSION.SDK_INT >= 31) {
                @Suppress("DEPRECATION")
                val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                if (!sizes.isNullOrEmpty()) {
                    val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
                    val wide = sizes.filter { it.width >= it.height && it.height >= 48f }
                    val picked = if (portrait) wide.minByOrNull { it.width } else wide.maxByOrNull { it.width }
                    if (picked != null) return picked.height
                }
            }
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).toFloat()
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val primary = if (portrait) maxH else minH
            val secondary = if (portrait) minH else maxH
            return when {
                primary in 48f..160f -> primary
                secondary in 48f..160f -> secondary
                else -> 83f
            }
        }

        private fun progressRing(context: Context, percent: Float, diameterDp: Float): Bitmap {
            val metrics = context.resources.displayMetrics
            val size = (diameterDp * metrics.density).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            bitmap.density = metrics.densityDpi
            val canvas = Canvas(bitmap)
            val stroke = 6f * metrics.density
            val inset = stroke / 2f
            val oval = RectF(inset, inset, size - inset, size - inset)
            val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                color = Color.parseColor(ACCENT)
                alpha = 51
            }
            canvas.drawArc(oval, -90f, 360f, false, track)
            val sweep = (percent.coerceIn(0f, 100f) / 100f) * 360f
            if (sweep > 0.4f) {
                val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke
                    strokeCap = Paint.Cap.ROUND
                    color = Color.parseColor(ACCENT)
                }
                canvas.drawArc(oval, -90f, if (sweep >= 359.5f) 359.5f else sweep, false, arc)
            }
            val label = FitnessData.formatProgress(percent)
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11f, metrics)
            }
            val baseline = size / 2f - (text.descent() + text.ascent()) / 2f
            canvas.drawText(label, size / 2f, baseline, text)
            return bitmap
        }
    }
}
