package de.nyxnord.kraftlog.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.data.local.entity.BodyWeightEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(app: KraftLogApplication) {
    val vm: WeightViewModel = viewModel(
        factory = WeightViewModel.factory(app.bodyWeightRepository)
    )
    val entries by vm.entries.collectAsState()  // sorted DESC by date
    val chronological = remember(entries) { entries.sortedBy { it.date } }

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Body Weight") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add entry")
            }
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No entries yet. Tap + to log your weight.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            WeightContent(
                entries = entries,
                chronological = chronological,
                onDelete = { vm.deleteEntry(it) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (showDialog) {
        AddWeightDialog(
            onConfirm = { kg, dateMs ->
                vm.addEntry(kg, dateMs)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun WeightContent(
    entries: List<BodyWeightEntry>,
    chronological: List<BodyWeightEntry>,
    onDelete: (BodyWeightEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { System.currentTimeMillis() }
    val ms7d = 7L * 24 * 3600 * 1000
    val ms30d = 30L * 24 * 3600 * 1000

    val latest = entries[0].weightKg
    val minW = entries.minOf { it.weightKg }
    val maxW = entries.maxOf { it.weightKg }
    val avgW = entries.map { it.weightKg }.average().toFloat()
    val totalChange = if (entries.size > 1) latest - entries.last().weightKg else null

    val change7d = remember(entries) {
        entries.drop(1).minByOrNull { abs(it.date - (now - ms7d)) }?.let { latest - it.weightKg }
    }
    val change30d = remember(entries) {
        entries.drop(1).minByOrNull { abs(it.date - (now - ms30d)) }?.let { latest - it.weightKg }
    }

    // Limit chart to last 60 entries for readability
    val chartEntries = if (chronological.size > 60) chronological.takeLast(60) else chronological

    LazyColumn(modifier = modifier.fillMaxSize()) {

        // ── Current weight header ─────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(
                    "Current",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatKg(latest),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "kg",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    dateFormat.format(Date(entries[0].date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Change stats ──────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeltaCard(label = "7 days", delta = change7d, modifier = Modifier.weight(1f))
                DeltaCard(label = "30 days", delta = change30d, modifier = Modifier.weight(1f))
                DeltaCard(label = "All time", delta = totalChange, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Min / Avg / Max ───────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(label = "Min", value = "${formatKg(minW)} kg", modifier = Modifier.weight(1f))
                StatCard(label = "Avg", value = "${formatKg(avgW)} kg", modifier = Modifier.weight(1f))
                StatCard(label = "Max", value = "${formatKg(maxW)} kg", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Chart ─────────────────────────────────────────────────────────────
        if (chartEntries.size >= 2) {
            item {
                Text(
                    "Trend" + if (entries.size > 60) " (last 60 entries)" else "",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(4.dp))
                WeightChart(entries = chartEntries)
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── History ───────────────────────────────────────────────────────────
        item {
            HorizontalDivider()
            Text(
                "History",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(entries, key = { it.id }) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${formatKg(entry.weightKg)} kg",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${dateFormat.format(Date(entry.date))}, ${timeFormat.format(Date(entry.date))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onDelete(entry) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun DeltaCard(label: String, delta: Float?, modifier: Modifier = Modifier) {
    val color = when {
        delta == null -> MaterialTheme.colorScheme.onSurfaceVariant
        delta < 0 -> MaterialTheme.colorScheme.primary
        delta > 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (delta != null) "${if (delta >= 0) "+" else ""}${formatKg(delta)} kg" else "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun WeightChart(entries: List<BodyWeightEntry>) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val labelStyle = MaterialTheme.typography.labelSmall

    val minW = entries.minOf { it.weightKg }
    val maxW = entries.maxOf { it.weightKg }
    val range = (maxW - minW).coerceAtLeast(0.5f)
    val pad = range * 0.2f
    val yMin = minW - pad
    val yMax = maxW + pad
    val n = entries.size

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
    ) {
        val leftPad = 44.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 8.dp.toPx()
        val bottomPad = 24.dp.toPx()

        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        fun xOf(i: Int) = leftPad + (i.toFloat() / (n - 1).toFloat()) * chartW
        fun yOf(w: Float) = topPad + (1f - (w - yMin) / (yMax - yMin)) * chartH

        // Horizontal grid lines with weight labels
        val gridCount = 4
        repeat(gridCount) { g ->
            val fraction = g.toFloat() / (gridCount - 1).toFloat()
            val y = topPad + fraction * chartH
            val wAtY = yMax - fraction * (yMax - yMin)

            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartW, y),
                strokeWidth = 1.dp.toPx()
            )

            val labelResult = textMeasurer.measure(formatKg(wAtY), labelStyle)
            drawText(
                labelResult,
                color = labelColor,
                topLeft = Offset(
                    x = leftPad - labelResult.size.width - 4.dp.toPx(),
                    y = y - labelResult.size.height / 2f
                )
            )
        }

        // Gradient fill below the line
        val fillPath = Path().apply {
            entries.forEachIndexed { i, e ->
                val x = xOf(i); val y = yOf(e.weightKg)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            lineTo(xOf(n - 1), topPad + chartH)
            lineTo(xOf(0), topPad + chartH)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                startY = topPad,
                endY = topPad + chartH
            )
        )

        // Line
        val linePath = Path().apply {
            entries.forEachIndexed { i, e ->
                val x = xOf(i); val y = yOf(e.weightKg)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            linePath,
            color = primaryColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Dots (skip individual dots when there are many entries to avoid clutter)
        if (n <= 30) {
            entries.forEachIndexed { i, e ->
                val center = Offset(xOf(i), yOf(e.weightKg))
                drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = center)
                drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = center)
            }
        } else {
            // Only draw first and last dots
            listOf(0, n - 1).forEach { i ->
                val center = Offset(xOf(i), yOf(entries[i].weightKg))
                drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = center)
                drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = center)
            }
        }

        // X-axis date labels (first / last)
        val xLabelY = topPad + chartH + 4.dp.toPx()
        val firstLabel = textMeasurer.measure(dateFormat.format(Date(entries.first().date)), labelStyle)
        val lastLabel = textMeasurer.measure(dateFormat.format(Date(entries.last().date)), labelStyle)
        drawText(firstLabel, color = labelColor, topLeft = Offset(xOf(0), xLabelY))
        drawText(lastLabel, color = labelColor, topLeft = Offset(xOf(n - 1) - lastLabel.size.width, xLabelY))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWeightDialog(
    onConfirm: (Float, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val now = remember { Calendar.getInstance() }

    var input by remember { mutableStateOf("") }
    val kg = input.replace(",", ".").toFloatOrNull()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = run {
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            utcCal.set(Calendar.MILLISECOND, 0)
            utcCal.timeInMillis
        }
    )
    val timePickerState = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = true
    )

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val selectedDateLabel = remember(datePickerState.selectedDateMillis) {
        val ms = datePickerState.selectedDateMillis ?: return@remember ""
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = ms }
        val localCal = Calendar.getInstance().apply {
            set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        dateFormat.format(Date(localCal.timeInMillis))
    }
    val selectedTimeLabel = remember(timePickerState.hour, timePickerState.minute) {
        "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDateLabel,
                        onValueChange = {},
                        label = { Text("Date") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedTimeLabel,
                        onValueChange = {},
                        label = { Text("Time") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.Schedule, null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showTimePicker = true })
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val w = kg ?: return@TextButton
                    val utcMs = datePickerState.selectedDateMillis ?: return@TextButton
                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
                    val finalMs = Calendar.getInstance().apply {
                        set(
                            utcCal.get(Calendar.YEAR),
                            utcCal.get(Calendar.MONTH),
                            utcCal.get(Calendar.DAY_OF_MONTH),
                            timePickerState.hour,
                            timePickerState.minute,
                            0
                        )
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    onConfirm(w, finalMs)
                },
                enabled = kg != null && kg > 0
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatKg(kg: Float): String =
    if (kg % 1f == 0f) "${kg.toInt()}" else "${"%.1f".format(kg)}"
