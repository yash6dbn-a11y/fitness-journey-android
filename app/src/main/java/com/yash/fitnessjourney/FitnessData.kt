package com.yash.fitnessjourney

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeighIn(val date: Long, val weight: Float)

object FitnessData {
    private const val PREFS = "fitness_journey_data"
    private const val KEY_START = "start_weight"
    private const val KEY_CURRENT = "current_weight"
    private const val KEY_TARGET = "target_weight"
    private const val KEY_HISTORY = "history"

    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun start(context: Context) = prefs(context).getFloat(KEY_START, 101.4f)
    fun current(context: Context) = prefs(context).getFloat(KEY_CURRENT, 94.55f)
    fun target(context: Context) = prefs(context).getFloat(KEY_TARGET, 76f)

    fun updateGoals(context: Context, start: Float, target: Float) {
        prefs(context).edit().putFloat(KEY_START, start).putFloat(KEY_TARGET, target).apply()
        FitnessWidgetProvider.updateAll(context)
    }

    fun history(context: Context): List<WeighIn> {
        val raw = prefs(context).getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                array.getJSONObject(i).let {
                    WeighIn(it.getLong("date"), it.getDouble("weight").toFloat())
                }
            }.sortedByDescending { it.date }
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, weight: Float, date: Long = System.currentTimeMillis()) {
        require(weight.isFinite() && weight in 30f..300f) { "Enter a valid weight between 30 and 300 kg." }
        val p = prefs(context)
        val list = history(context).filterNot { sameDay(it.date, date) }.toMutableList()
        list.add(WeighIn(date, weight))
        val array = JSONArray()
        list.sortedBy { it.date }.forEach {
            array.put(JSONObject().put("date", it.date).put("weight", it.weight.toDouble()))
        }
        p.edit().putFloat(KEY_CURRENT, weight).putString(KEY_HISTORY, array.toString()).apply()
        FitnessWidgetProvider.updateAll(context)
    }

    fun progress(context: Context): Float {
        val span = start(context) - target(context)
        if (span <= 0f) return 0f
        return (((start(context) - current(context)) / span) * 100f).coerceIn(0f, 100f)
    }

    fun lost(context: Context) = (start(context) - current(context)).coerceAtLeast(0f)
    fun remaining(context: Context) = (current(context) - target(context)).coerceAtLeast(0f)
    fun formatDate(time: Long): String = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(time))

    fun exportCsv(context: Context): String {
        val rows = history(context).sortedBy { it.date }
        return buildString {
            append("date,weight_kg\n")
            rows.forEach { append("\"${formatDate(it.date)}\",${String.format(Locale.US, "%.2f", it.weight)}\n") }
        }
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val f = SimpleDateFormat("yyyyMMdd", Locale.US)
        return f.format(Date(a)) == f.format(Date(b))
    }
}
