package de.nyxnord.kraftlog.ui.exercises

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.ExerciseCategory
import de.nyxnord.kraftlog.data.local.entity.MuscleGroup
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExercisesScreen(
    app: KraftLogApplication,
    onExerciseClick: (Long) -> Unit,
    pickerMode: Boolean = false
) {
    val vm: ExercisesViewModel = viewModel(
        factory = ExercisesViewModel.factory(app.exerciseRepository, app.workoutRepository)
    )
    val exercises by vm.exercises.collectAsState()
    val query by vm.searchQuery.collectAsState()
    val categoryFilter by vm.categoryFilter.collectAsState()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pickerMode) "Pick an Exercise" else "Exercises") }
            )
        },
        floatingActionButton = {
            if (!pickerMode) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Add exercise")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = { vm.searchQuery.value = it },
                placeholder = { Text("Search exercises…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            // Category filter chips
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { vm.categoryFilter.value = null },
                    label = { Text("All") }
                )
                ExerciseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = {
                            vm.categoryFilter.value = if (categoryFilter == cat) null else cat
                        },
                        label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(exercises, key = { it.id }) { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = {
                            Text(
                                "${exercise.category.name.lowercase().replaceFirstChar { it.uppercase() }} · " +
                                    exercise.primaryMuscles.joinToString(", ") {
                                        it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                                    }
                            )
                        },
                        modifier = Modifier.clickable { onExerciseClick(exercise.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, category, muscles, instructions ->
                vm.saveCustomExercise(name, category, muscles, instructions)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ExerciseCategory, List<MuscleGroup>, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ExerciseCategory.STRENGTH) }
    val selectedMuscles = remember { mutableStateListOf<MuscleGroup>() }
    var instructions by rememberSaveable { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ExerciseCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    "Muscles (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MuscleGroup.entries.filter { it != MuscleGroup.FULL_BODY }.forEach { muscle ->
                        FilterChip(
                            selected = muscle in selectedMuscles,
                            onClick = {
                                if (muscle in selectedMuscles) selectedMuscles.remove(muscle)
                                else selectedMuscles.add(muscle)
                            },
                            label = { Text(muscle.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, category, selectedMuscles.toList(), instructions) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    app: KraftLogApplication,
    onBack: () -> Unit
) {
    val vm: ExercisesViewModel = viewModel(
        factory = ExercisesViewModel.factory(app.exerciseRepository, app.workoutRepository)
    )
    val detailState by remember(exerciseId) { vm.getDetailState(exerciseId) }.collectAsState()
    val exercise = detailState.exercise

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            exercise?.let { ex ->
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        ex.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            MuscleDiagram(
                                primaryMuscles = ex.primaryMuscles,
                                secondaryMuscles = ex.secondaryMuscles
                            )
                            if (ex.primaryMuscles.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "Primary:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        ex.primaryMuscles.joinToString(", ") {
                                            it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            if (ex.secondaryMuscles.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "Secondary:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        ex.secondaryMuscles.joinToString(", ") {
                                            it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
                if (ex.instructions.isNotBlank()) {
                    item {
                        Text("Instructions", style = MaterialTheme.typography.titleSmall)
                        Text(ex.instructions)
                    }
                }
            }

            detailState.records?.let { records ->
                item {
                    RecordsCard(records = records)
                }
            }

            if (detailState.recentSets.isNotEmpty()) {
                item {
                    WeightProgressChart(sets = detailState.recentSets)
                }
                item {
                    Text("Recent Sets", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp))
                }
                // Group by session (loggedAt proximity) — show last 20 sets
                items(detailState.recentSets.take(20)) { set ->
                    SetHistoryRow(set)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun RecordsCard(records: ExerciseRecords) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Lifetime Records", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (records.maxWeightKg != null) {
                    RecordStatTile(
                        label = "Best Weight",
                        value = formatWeight(records.maxWeightKg) + " kg"
                    )
                }
                if (records.bestEstimated1RM != null) {
                    RecordStatTile(
                        label = "Est. 1RM",
                        value = formatWeight(records.bestEstimated1RM) + " kg"
                    )
                }
                if (records.maxRepsInSet != null && records.maxWeightKg == null) {
                    // Bodyweight-only exercise — show max reps prominently
                    RecordStatTile(label = "Max Reps", value = "${records.maxRepsInSet}")
                }
                RecordStatTile(label = "Sessions", value = "${records.totalSessions}")
                RecordStatTile(
                    label = "Volume",
                    value = if (records.totalVolumeKg >= 1_000f)
                        "${"%.1f".format(records.totalVolumeKg / 1_000f)} t"
                    else
                        "${records.totalVolumeKg.toInt()} kg"
                )
            }
        }
    }
}

@Composable
private fun RecordStatTile(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatWeight(kg: Float): String =
    if (kg % 1f == 0f) "${kg.toInt()}" else "${"%.1f".format(kg)}"

@Composable
private fun WeightProgressChart(sets: List<WorkoutSet>) {
    // Group by session, get max weight per session, sorted oldest→newest, last 10 sessions
    val dataPoints = sets
        .groupBy { it.sessionId }
        .mapValues { (_, s) -> s.maxByOrNull { it.weightKg } }
        .values
        .filterNotNull()
        .filter { !it.isBodyweight }
        .sortedBy { it.loggedAt }
        .takeLast(10)

    if (dataPoints.size < 2) return

    val maxWeight = dataPoints.maxOf { it.weightKg }
    val minWeight = dataPoints.minOf { it.weightKg }
    val weightRange = (maxWeight - minWeight).coerceAtLeast(1f)
    val dateFormat = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Progress", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            val lineColor = MaterialTheme.colorScheme.primary
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val padL = 52f
                val padR = 16f
                val padT = 16f
                val padB = 36f
                val chartW = size.width - padL - padR
                val chartH = size.height - padT - padB
                val xStep = chartW / (dataPoints.size - 1)

                val points = dataPoints.mapIndexed { i, s ->
                    Offset(
                        x = padL + i * xStep,
                        y = padT + chartH - (s.weightKg - minWeight) / weightRange * chartH
                    )
                }

                // Grid line at top and bottom
                drawLine(
                    color = labelColor.copy(alpha = 0.2f),
                    start = Offset(padL, padT),
                    end = Offset(padL + chartW, padT),
                    strokeWidth = 1f
                )
                drawLine(
                    color = labelColor.copy(alpha = 0.2f),
                    start = Offset(padL, padT + chartH),
                    end = Offset(padL + chartW, padT + chartH),
                    strokeWidth = 1f
                )

                // Line segments
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.5f
                    )
                }

                // Dots
                points.forEach { pt ->
                    drawCircle(color = lineColor, radius = 5f, center = pt)
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.White,
                        radius = 2.5f,
                        center = pt
                    )
                }

                // Y-axis labels
                val yPaint = android.graphics.Paint().apply {
                    textSize = 10.sp.toPx()
                    color = labelColor.toArgb()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "${maxWeight.toInt()} kg",
                    padL - 6f, padT + yPaint.textSize * 0.4f, yPaint
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${minWeight.toInt()} kg",
                    padL - 6f, padT + chartH + yPaint.textSize * 0.4f, yPaint
                )

                // X-axis date labels — skip every other if crowded
                val skipStep = if (dataPoints.size > 6) 2 else 1
                val xPaint = android.graphics.Paint().apply {
                    textSize = 10.sp.toPx()
                    color = labelColor.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                dataPoints.forEachIndexed { i, s ->
                    if (i % skipStep == 0 || i == dataPoints.size - 1) {
                        drawContext.canvas.nativeCanvas.drawText(
                            dateFormat.format(java.util.Date(s.loggedAt)),
                            padL + i * xStep,
                            size.height - 4f,
                            xPaint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetHistoryRow(set: WorkoutSet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
                .format(java.util.Date(set.loggedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (set.isBodyweight) "${set.reps} reps"
            else "${set.weightKg} kg × ${set.reps}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
