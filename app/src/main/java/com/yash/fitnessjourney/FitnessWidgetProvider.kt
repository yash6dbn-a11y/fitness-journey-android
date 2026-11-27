package com.yash.fitnessjourney

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
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
        private const val TRACK = "#344052"
        private const val LABEL = "#F7F9FC"
        private const val CELL = "#212342"
        private const val MUTED = "#D5DDE8"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FitnessWidgetProvider::class.java))
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_fitness_progress)
            val current = FitnessData.current(context)
            val lost = FitnessData.lost(context)
            val remaining = FitnessData.remaining(context)
            val progress = FitnessData.progress(context)
            views.setTextViewText(R.id.current_weight, weightLine(current))
            views.setTextViewText(R.id.progress_detail, detailLine(lost, remaining))
            views.setTextViewText(R.id.target_label, "TARGET ${formatKg(FitnessData.target(context))} kg")
            views.setImageViewBitmap(R.id.progress_ring, progressRing(context, progress))
            val intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, intent)
            manager.updateAppWidget(id, views)
        }

        private fun weightLine(current: Float): CharSequence {
            val number = formatKg(current)
            return SpannableString("$number kg").apply {
                setSpan(RelativeSizeSpan(0.58f), number.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(Color.parseColor(MUTED)), number.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        private fun detailLine(lost: Float, remaining: Float): CharSequence {
            val text = String.format(
                Locale.getDefault(),
                "↓ %s kg lost · %s kg to goal",
                formatKg(lost),
                formatKg(remaining)
            )
            return SpannableString(text).apply {
                setSpan(
                    ForegroundColorSpan(Color.parseColor(ACCENT)),
                    0,
                    1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
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

        private fun progressRing(context: Context, percent: Float): Bitmap {
            val metrics = context.resources.displayMetrics
            val size = (53f * metrics.density).toInt().coerceIn(48, 512)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            bitmap.density = metrics.densityDpi
            val canvas = Canvas(bitmap)
            val cell = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(CELL) }
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - 0.5f, cell)
            val inset = size * 0.17f
            val oval = RectF(inset, inset, size - inset, size - inset)
            val stroke = oval.width() * 0.105f
            val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                color = Color.parseColor(TRACK)
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(oval, -90f, 360f, false, track)
            val sweep = (percent.coerceIn(0f, 100f) / 100f) * 360f
            if (sweep > 0.4f) {
                val drawSweep = if (sweep >= 359.5f) 359.5f else sweep
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke * 1.7f
                    color = Color.parseColor(ACCENT)
                    alpha = 80
                    strokeCap = Paint.Cap.ROUND
                    maskFilter = BlurMaskFilter(stroke * 0.5f, BlurMaskFilter.Blur.NORMAL)
                }
                val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = stroke
                    color = Color.parseColor(ACCENT)
                    strokeCap = Paint.Cap.ROUND
                }
                try {
                    canvas.drawArc(oval, -90f, drawSweep, false, glow)
                } catch (_: RuntimeException) {
                    // The solid arc below still shows progress if blur is unavailable.
                }
                canvas.drawArc(oval, -90f, drawSweep, false, arc)
            }
            val label = FitnessData.formatProgress(percent)
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(LABEL)
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textSize = oval.width() * 0.28f
            }
            val inner = oval.width() - stroke
            while (text.measureText(label) > inner * 0.86f && text.textSize > oval.width() * 0.16f) {
                text.textSize *= 0.92f
            }
            val baseline = size / 2f - (text.descent() + text.ascent()) / 2f
            canvas.drawText(label, size / 2f, baseline, text)
            return bitmap
        }
    }
}
