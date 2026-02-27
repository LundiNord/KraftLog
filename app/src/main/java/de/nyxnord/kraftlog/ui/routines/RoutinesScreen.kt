package de.nyxnord.kraftlog.ui.routines

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise
import de.nyxnord.kraftlog.data.local.relation.RoutineExerciseWithExercise
import de.nyxnord.kraftlog.data.local.relation.RoutineWithExerciseDetails
import kotlinx.coroutines.launch

// ── List ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    app: KraftLogApplication,
    onRoutineClick: (Long) -> Unit,
    onCreateRoutine: () -> Unit
) {
    val vm: RoutinesViewModel = viewModel(factory = RoutinesViewModel.factory(app.routineRepository))
    val routines by vm.routines.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImportError by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val json = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            }
            if (json == null || !vm.importRoutineFromJson(json, app.exerciseRepository)) {
                showImportError = true
            }
        }
    }

    if (showImportError) {
        AlertDialog(
            onDismissRequest = { showImportError = false },
            title = { Text("Import Failed") },
            text = { Text("The file could not be imported. Make sure it is a valid KraftLog routine file.") },
            confirmButton = {
                TextButton(onClick = { showImportError = false }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routines") },
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                        Icon(Icons.Default.FileDownload, "Import routine")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoutine) {
                Icon(Icons.Default.Add, "Create routine")
            }
        }
    ) { innerPadding ->
        if (routines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No routines yet.\nTap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                itemsIndexed(routines, key = { _, r -> r.routine.id }) { _, routineWithDetails ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                vm.deleteRoutine(routineWithDetails.routine)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    ) {
                        RoutineListItem(routineWithDetails, onClick = { onRoutineClick(routineWithDetails.routine.id) })
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun RoutineListItem(
    routineWithDetails: RoutineWithExerciseDetails,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(routineWithDetails.routine.name) },
        supportingContent = {
            Text("${routineWithDetails.exerciseDetails.size} exercises")
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// ── Detail ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routineId: Long,
    app: KraftLogApplication,
    onStartWorkout: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit = onBack
) {
    val vm: RoutinesViewModel = viewModel(factory = RoutinesViewModel.factory(app.routineRepository))
    val routineDetail by vm.getRoutineDetail(routineId).collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Routine?") },
            text = { Text("\"${routineDetail?.routine?.name ?: "This routine"}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        routineDetail?.routine?.let { vm.deleteRoutine(it) }
                        showDeleteDialog = false
                        onDeleted()
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val json = vm.exportRoutineJson(routineId)
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routineDetail?.routine?.name ?: "Routine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val filename = (routineDetail?.routine?.name ?: "routine")
                            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
                        exportLauncher.launch("$filename.json")
                    }) {
                        Icon(Icons.Default.Share, "Export routine")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete routine",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onStartWorkout) {
                Icon(Icons.Default.PlayArrow, "Start workout")
            }
        }
    ) { innerPadding ->
        val detail = routineDetail
        if (detail == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Loading…")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (detail.routine.description.isNotBlank()) {
                item {
                    Text(
                        detail.routine.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            val sorted = detail.exerciseDetails.sortedBy { it.routineExercise.orderIndex }
            items(sorted.size) { idx ->
                val item = sorted[idx]
                ExerciseTargetCard(item)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ExerciseTargetCard(item: RoutineExerciseWithExercise) {
    val re = item.routineExercise
    val weights = re.targetWeightsPerSet.split(",").filter { it.isNotBlank() }
    val repsPerSet = re.targetRepsPerSet.split(",").filter { it.isNotBlank() }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${re.targetSets} sets", style = MaterialTheme.typography.bodySmall)
                if (repsPerSet.isEmpty()) Text("${re.targetReps} reps", style = MaterialTheme.typography.bodySmall)
                Text("${re.restSeconds}s rest", style = MaterialTheme.typography.bodySmall)
            }
            if (weights.isNotEmpty() || repsPerSet.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    (0 until re.targetSets).joinToString("  ·  ") { i ->
                        val w = weights.getOrNull(i)
                        val r = repsPerSet.getOrElse(i) { re.targetReps.toString() }
                        buildString {
                            append("S${i + 1}:")
                            if (!w.isNullOrBlank()) append(" ${w}kg")
                            append(" ×${r}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Edit / Create ─────────────────────────────────────────────────────────────

data class EditableRoutineExercise(
    val exerciseId: Long,
    val exerciseName: String,
    var sets: String = "3",
    var setWeights: List<String> = List(3) { "" },
    var setReps: List<String> = List(3) { "" },
    var restSeconds: String = "90"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditScreen(
    routineId: Long,
    app: KraftLogApplication,
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val vm: RoutinesViewModel = viewModel(factory = RoutinesViewModel.factory(app.routineRepository))
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val exerciseItems = remember { mutableStateListOf<EditableRoutineExercise>() }
    var showExercisePicker by remember { mutableStateOf(false) }

    // Load existing routine data once when editing
    LaunchedEffect(routineId) {
        if (routineId != -1L) {
            val detail = vm.loadRoutineForEdit(routineId) ?: return@LaunchedEffect
            name = detail.routine.name
            description = detail.routine.description
            exerciseItems.clear()
            detail.exerciseDetails.sortedBy { it.routineExercise.orderIndex }.forEach { item ->
                val re = item.routineExercise
                val setCount = re.targetSets
                val parsedWeights = re.targetWeightsPerSet.split(",").filter { it.isNotBlank() }
                val parsedReps = re.targetRepsPerSet.split(",").filter { it.isNotBlank() }
                exerciseItems.add(
                    EditableRoutineExercise(
                        exerciseId = item.exercise.id,
                        exerciseName = item.exercise.name,
                        sets = setCount.toString(),
                        setWeights = List(setCount) { i -> parsedWeights.getOrElse(i) { "" } },
                        setReps = List(setCount) { i -> parsedReps.getOrElse(i) { re.targetReps.toString() } },
                        restSeconds = re.restSeconds.toString()
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (routineId == -1L) "New Routine" else "Edit Routine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val routineExercises = exerciseItems.mapIndexed { idx, item ->
                                    RoutineExercise(
                                        routineId = if (routineId == -1L) 0L else routineId,
                                        exerciseId = item.exerciseId,
                                        orderIndex = idx,
                                        targetSets = item.sets.toIntOrNull() ?: 3,
                                        targetReps = item.setReps.firstOrNull { it.isNotBlank() }?.toIntOrNull() ?: 10,
                                        targetWeightKg = item.setWeights.firstOrNull { it.isNotBlank() }?.toFloatOrNull(),
                                        targetWeightsPerSet = item.setWeights.joinToString(","),
                                        targetRepsPerSet = item.setReps.joinToString(","),
                                        restSeconds = item.restSeconds.toIntOrNull() ?: 90
                                    )
                                }
                                val savedId = vm.saveRoutine(routineId, name, description, routineExercises)
                                onSaved(savedId)
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
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
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Routine Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            if (exerciseItems.isNotEmpty()) {
                item {
                    Text("Exercises", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }

            itemsIndexed(exerciseItems) { idx, item ->
                EditableExerciseCard(
                    item = item,
                    onRemove = { exerciseItems.removeAt(idx) },
                    onUpdate = { updated -> exerciseItems[idx] = updated }
                )
            }

            item {
                Button(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Text("Add Exercise", modifier = Modifier.padding(start = 8.dp))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            app = app,
            onPicked = { exercise ->
                exerciseItems.add(EditableRoutineExercise(exerciseId = exercise.id, exerciseName = exercise.name))
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false }
        )
    }
}

@Composable
private fun EditableExerciseCard(
    item: EditableRoutineExercise,
    onRemove: () -> Unit,
    onUpdate: (EditableRoutineExercise) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.exerciseName, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.sets,
                    onValueChange = { newSets ->
                        val count = newSets.toIntOrNull()?.coerceIn(1, 20) ?: item.setWeights.size
                        val newWeights = List(count) { i -> item.setWeights.getOrElse(i) { "" } }
                        val newReps = List(count) { i -> item.setReps.getOrElse(i) { "" } }
                        onUpdate(item.copy(sets = newSets, setWeights = newWeights, setReps = newReps))
                    },
                    label = { Text("Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = item.restSeconds,
                    onValueChange = { onUpdate(item.copy(restSeconds = it)) },
                    label = { Text("Rest (s)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.width(52.dp))
                Text("kg", style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f))
                Text("Reps", style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f))
            }
            item.setWeights.forEachIndexed { setIdx, w ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Set ${setIdx + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(52.dp)
                    )
                    OutlinedTextField(
                        value = w,
                        onValueChange = { newW ->
                            val newWeights = item.setWeights.toMutableList().also { it[setIdx] = newW }
                            onUpdate(item.copy(setWeights = newWeights))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = item.setReps.getOrElse(setIdx) { "" },
                        onValueChange = { newR ->
                            val newReps = item.setReps.toMutableList().also { it[setIdx] = newR }
                            onUpdate(item.copy(setReps = newReps))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerDialog(
    app: KraftLogApplication,
    onPicked: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val vm: de.nyxnord.kraftlog.ui.exercises.ExercisesViewModel = viewModel(
        factory = de.nyxnord.kraftlog.ui.exercises.ExercisesViewModel.factory(
            app.exerciseRepository, app.workoutRepository
        )
    )
    val exercises by vm.exercises.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(query) { vm.searchQuery.value = query }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick an Exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(exercises.size) { idx ->
                        val ex = exercises[idx]
                        ListItem(
                            headlineContent = { Text(ex.name) },
                            supportingContent = {
                                Text(ex.primaryMuscles.joinToString(", ") {
                                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                                })
                            },
                            modifier = Modifier.clickable { onPicked(ex) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
