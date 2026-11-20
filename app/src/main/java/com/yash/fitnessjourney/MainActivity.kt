package com.yash.fitnessjourney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val Ink = Color(0xFF080D18)
private val Panel = Color(0xFF111B2B)
private val Green = Color(0xFF69F800)
private val Muted = Color(0xFFAAB5C6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FitnessJourneyApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessJourneyApp(context: android.content.Context) {
    var refresh by remember { mutableIntStateOf(0) }
    var weightText by remember { mutableStateOf("") }
    var showEntry by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val current = remember(refresh) { FitnessData.current(context) }
    val lost = remember(refresh) { FitnessData.lost(context) }
    val remaining = remember(refresh) { FitnessData.remaining(context) }
    val progress = remember(refresh) { FitnessData.progress(context) }
    val history = remember(refresh) { FitnessData.history(context) }
    val start = remember(refresh) { FitnessData.start(context) }
    val target = remember(refresh) { FitnessData.target(context) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(FitnessData.exportCsv(context)) }
            }.onFailure { errorMessage = "Could not export the CSV file." }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Ink, surface = Panel, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Color.White) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Rounded.AutoGraph, null) }, label = { Text("Overview") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Rounded.History, null) }, label = { Text("History") })
            }
        }, floatingActionButton = {
            FloatingActionButton(onClick = { weightText = ""; errorMessage = null; showEntry = true }, containerColor = Green, contentColor = Ink) { Icon(Icons.Rounded.Add, "Log weight") }
        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 26.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("YOUR PROGRESS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(if (selectedTab == 0) "A little better\nevery week." else "Your journey,\nin numbers.", color = Color.White, fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Rounded.Settings, "Goals and settings", tint = Muted) }
                    }
                }
                if (selectedTab == 0) {
                    item {
                        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF17263B), Panel, Color(0xFF0C1422))), RoundedCornerShape(28.dp)).padding(22.dp)) {
                            Text("CURRENT WEIGHT", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(String.format(Locale.getDefault(), "%.2f", current), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(7.dp)); Text("kg", color = Muted, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricCard("LOST", String.format(Locale.getDefault(), "%.2f kg", lost), Green, Modifier.weight(1f))
                                MetricCard("TO GO", String.format(Locale.getDefault(), "%.2f kg", remaining), Color.White, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(22.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Journey progress", color = Color.White, fontWeight = FontWeight.Medium)
                                Text("${progress.toInt()}%", color = Green, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(9.dp), color = Green, trackColor = Color(0xFF303B4D))
                            Spacer(Modifier.height(9.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${formatWeight(start)} kg start", color = Muted, fontSize = 12.sp)
                                Text("${formatWeight(target)} kg goal", color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoCard("NEXT MILESTONE", nextMilestone(current, target), "Keep building momentum", Modifier.weight(1f))
                            InfoCard("WEEKLY CHECK-IN", "Sunday", "Update after weighing in", Modifier.weight(1f))
                        }
                    }
                    item {
                        Text("Recent check-ins", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        if (history.isEmpty()) Text("Your weigh-ins will appear here.", color = Muted, fontSize = 14.sp)
                    }
                    items(history.take(3)) { entry -> HistoryRow(entry) }
                } else {
                    item { Text("Starting at ${formatWeight(start)} kg · Goal ${formatWeight(target)} kg", color = Muted, fontSize = 14.sp) }
                    if (history.size >= 2) item { WeightTrendChart(history = history) }
                    if (history.isEmpty()) item { EmptyHistory() }
                    items(history) { entry -> HistoryRow(entry) }
                    item {
                        OutlinedButton(onClick = { exportLauncher.launch("fitness-journey-history.csv") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text("Export weigh-ins as CSV")
                        }
                    }
                }
            }
        }

        if (showEntry) {
            AlertDialog(onDismissRequest = { showEntry = false }, title = { Text("Log weigh-in") }, text = {
                Column {
                    Text("Enter your latest weight in kilograms.", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = weightText, onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(6) }, label = { Text("Weight (kg)") }, singleLine = true, isError = errorMessage != null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            }, confirmButton = {
                TextButton(onClick = {
                    val value = weightText.toFloatOrNull()
                    if (value == null || value !in 30f..300f) errorMessage = "Enter a weight between 30 and 300 kg."
                    else {
                        FitnessData.save(context, value)
                        refresh++
                        showEntry = false
                    }
                }) { Text("Save check-in", color = Green) }
            }, dismissButton = { TextButton(onClick = { showEntry = false }) { Text("Cancel") } }, containerColor = Panel)
        }

        if (showSettings) {
            GoalSettingsDialog(startWeight = start, targetWeight = target, onDismiss = { showSettings = false }, onSave = { newStart, newTarget ->
                FitnessData.updateGoals(context, newStart, newTarget)
                refresh++
                showSettings = false
            })
        }
    }
}

@Composable
private fun GoalSettingsDialog(startWeight: Float, targetWeight: Float, onDismiss: () -> Unit, onSave: (Float, Float) -> Unit) {
    var startText by remember(startWeight) { mutableStateOf(formatWeight(startWeight)) }
    var targetText by remember(targetWeight) { mutableStateOf(formatWeight(targetWeight)) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Your goals") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = startText, onValueChange = { startText = it.filter { c -> c.isDigit() || c == '.' }.take(6) }, label = { Text("Starting weight (kg)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(value = targetText, onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' }.take(6) }, label = { Text("Target weight (kg)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        }
    }, confirmButton = {
        TextButton(onClick = {
            val s = startText.toFloatOrNull(); val t = targetText.toFloatOrNull()
            when {
                s == null || t == null || s !in 30f..300f || t !in 30f..300f -> error = "Both weights must be between 30 and 300 kg."
                t >= s -> error = "Target must be lower than starting weight."
                else -> onSave(s, t)
            }
        }) { Text("Save goals", color = Green) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }, containerColor = Panel)
}

@Composable
private fun WeightTrendChart(history: List<WeighIn>) {
    val points = history.sortedBy { it.date }.takeLast(12)
    val minWeight = (points.minOf { it.weight } - 1f).coerceAtLeast(0f)
    val maxWeight = points.maxOf { it.weight } + 1f
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(18.dp)) {
        Text("WEIGHT TREND", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val left = 12f; val right = size.width - 12f; val top = 8f; val bottom = size.height - 12f
            repeat(3) { i ->
                val y = top + (bottom - top) * i / 2f
                drawLine(Color(0xFF344052), Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            }
            if (points.size >= 2) {
                val path = Path()
                points.forEachIndexed { index, item ->
                    val x = left + (right - left) * index / (points.size - 1).toFloat()
                    val y = bottom - ((item.weight - minWeight) / (maxWeight - minWeight)) * (bottom - top)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, Green, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
                points.forEachIndexed { index, item ->
                    val x = left + (right - left) * index / (points.size - 1).toFloat()
                    val y = bottom - ((item.weight - minWeight) / (maxWeight - minWeight)) * (bottom - top)
                    drawCircle(Green, radius = 4.dp.toPx(), center = Offset(x, y))
                }
            }
        }
        Text("Showing up to your latest 12 saved weigh-ins", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun MetricCard(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(Color(0xFF0A1220), RoundedCornerShape(18.dp)).padding(14.dp)) {
        Text(label, color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(5.dp)); Text(value, color = tint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(20.dp)).padding(15.dp)) {
        Text(title, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(9.dp)); Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp)); Text(subtitle, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun HistoryRow(entry: WeighIn) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(Color(0xFF1C3020), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = Green, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(FitnessData.formatDate(entry.date), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("Weight check-in", color = Muted, fontSize = 11.sp)
        }
        Text(String.format(Locale.getDefault(), "%.2f kg", entry.weight), color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyHistory() {
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No check-ins yet", color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp)); Text("Log your next weigh-in to start your history.", color = Muted, fontSize = 13.sp)
    }
}

private fun formatWeight(value: Float) = String.format(Locale.getDefault(), "%.1f", value).removeSuffix(".0")

private fun nextMilestone(weight: Float, target: Float): String = when {
    weight <= target -> "Goal reached"
    weight > 90f -> "90 kg"
    weight > 85f -> "85 kg"
    weight > 80f -> "80 kg"
    weight > target -> "${formatWeight(target)} kg"
    else -> "Goal reached"
}
