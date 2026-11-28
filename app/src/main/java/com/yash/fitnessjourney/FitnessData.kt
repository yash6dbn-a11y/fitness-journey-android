package com.yash.fitnessjourney

import android.content.Context
import android.content.SharedPreferences
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
    private const val KEY_MIGRATED = "defaults_migrated_v2"

    const val DEFAULT_START = 101.0f
    const val DEFAULT_CURRENT = 96.6f
    const val DEFAULT_TARGET = 75.0f

    private const val OLD_START = 101.4f
    private const val OLD_CURRENT = 94.55f
    private const val OLD_TARGET = 76f
    private const val OLD_VALUE_TOLERANCE = 0.02f

    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Rewrites a stored weight only when it is still an untouched original seed
     * (101.4 / 94.55 / 76). Missing keys keep using the new defaults. History is
     * never written. Current weight is not changed once a weigh-in exists, so a
     * logged weight stays the source of truth. Returns true only the first time
     * this pass runs.
     */
    fun ensureDefaults(context: Context): Boolean {
        val stored = prefs(context)
        if (stored.getBoolean(KEY_MIGRATED, false)) return false
        val editor = stored.edit()
        migrateStoredDefault(stored, editor, KEY_START, OLD_START, DEFAULT_START)
        migrateStoredDefault(stored, editor, KEY_TARGET, OLD_TARGET, DEFAULT_TARGET)
        if (historyIsEmpty(stored)) {
            migrateStoredDefault(stored, editor, KEY_CURRENT, OLD_CURRENT, DEFAULT_CURRENT)
        }
        editor.putBoolean(KEY_MIGRATED, true).commit()
        return true
    }

    fun start(context: Context): Float {
        ensureDefaults(context)
        return prefs(context).getFloat(KEY_START, DEFAULT_START)
    }

    fun current(context: Context): Float {
        ensureDefaults(context)
        return prefs(context).getFloat(KEY_CURRENT, DEFAULT_CURRENT)
    }

    fun target(context: Context): Float {
        ensureDefaults(context)
        return prefs(context).getFloat(KEY_TARGET, DEFAULT_TARGET)
    }

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

    fun progress(context: Context): Float = progressPercent(start(context), current(context), target(context))

    fun progressPercent(startWeight: Float, currentWeight: Float, targetWeight: Float): Float {
        val span = startWeight - targetWeight
        if (span <= 0f || !span.isFinite()) return 0f
        val raw = ((startWeight - currentWeight) / span) * 100f
        if (!raw.isFinite()) return 0f
        return raw.coerceIn(0f, 100f)
    }

    fun formatProgress(percent: Float): String {
        val rounded = kotlin.math.round(percent * 10f) / 10f
        val whole = kotlin.math.round(rounded)
        return if (kotlin.math.abs(rounded - whole) < 0.05f) {
            String.format(Locale.getDefault(), "%.0f%%", whole)
        } else {
            String.format(Locale.getDefault(), "%.1f%%", rounded)
        }
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

    private fun historyIsEmpty(stored: SharedPreferences): Boolean {
        val raw = stored.getString(KEY_HISTORY, null)?.trim().orEmpty()
        return raw.isEmpty() || raw == "[]"
    }

    private fun migrateStoredDefault(
        stored: SharedPreferences,
        editor: SharedPreferences.Editor,
        key: String,
        oldValue: Float,
        newValue: Float
    ) {
        if (!stored.contains(key)) return
        val value = stored.getFloat(key, newValue)
        if (kotlin.math.abs(value - oldValue) <= OLD_VALUE_TOLERANCE) {
            editor.putFloat(key, newValue)
        }
    }
}
