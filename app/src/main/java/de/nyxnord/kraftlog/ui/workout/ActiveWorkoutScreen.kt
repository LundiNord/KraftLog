package de.nyxnord.kraftlog.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.ui.exercises.ExercisesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    routineId: Long,
    app: KraftLogApplication,
    onFinish: () -> Unit,
    onPickExercise: (Long) -> Unit = {}
) {
    val vm: ActiveWorkoutViewModel = viewModel(
        key = "active_workout_$routineId",
        factory = ActiveWorkoutViewModel.factory(
            routineId,
            app.routineRepository,
            app.workoutRepository,
            app.exerciseRepository
        )
    )
    val state by vm.uiState.collectAsState()
    var showExercisePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.sessionName)
                        Text(
                            formatElapsed(state.elapsedSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.Default.Close, "Discard")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(state.exercises) { exIdx, exercise ->
                ExerciseWorkoutCard(
                    exercise = exercise,
                    onSetRepsChange = { setIdx, value ->
                        vm.updateSetField(exIdx, setIdx, reps = value)
                    },
                    onSetWeightChange = { setIdx, value ->
                        vm.updateSetField(exIdx, setIdx, weight = value)
                    },
                    onLogSet = { setIdx -> vm.logSet(exIdx, setIdx) },
                    onAddSet = { vm.addSet(exIdx) }
                )
            }

            item {
                OutlinedButton(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null)
                    Text("Add Exercise", modifier = Modifier.padding(start = 8.dp))
                }
            }

            item {
                Button(
                    onClick = { vm.finishWorkout() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Finish Workout")
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            app = app,
            onPicked = { exerciseId, exerciseName ->
                vm.addExercise(exerciseId, exerciseName)
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerDialog(
    app: KraftLogApplication,
    onPicked: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    val vm: ExercisesViewModel = viewModel(
        factory = ExercisesViewModel.factory(app.exerciseRepository, app.workoutRepository)
    )
    val exercises by vm.exercises.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(query) { vm.searchQuery.value = query }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise") },
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
                    items(exercises, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = {
                                Text(exercise.primaryMuscles.joinToString(", ") {
                                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                                })
                            },
                            modifier = Modifier.clickable {
                                onPicked(exercise.id, exercise.name)
                            }
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

@Composable
private fun ExerciseWorkoutCard(
    exercise: LiveExercise,
    onSetRepsChange: (Int, String) -> Unit,
    onSetWeightChange: (Int, String) -> Unit,
    onLogSet: (Int) -> Unit,
    onAddSet: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Set", modifier = Modifier.weight(0.5f),
                    style = MaterialTheme.typography.labelSmall)
                Text("kg", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall)
                Text("Reps", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(0.6f))
            }

            exercise.sets.forEachIndexed { setIdx, set ->
                SetInputRow(
                    set = set,
                    onRepsChange = { onSetRepsChange(setIdx, it) },
                    onWeightChange = { onSetWeightChange(setIdx, it) },
                    onLog = { onLogSet(setIdx) }
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Text("Add Set", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun SetInputRow(
    set: LiveSet,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onLog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${set.setNumber}",
            modifier = Modifier.weight(0.5f),
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = set.weight,
            onValueChange = onWeightChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = !set.isLogged
        )
        OutlinedTextField(
            value = set.reps,
            onValueChange = onRepsChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = !set.isLogged
        )
        IconButton(
            onClick = onLog,
            enabled = !set.isLogged,
            modifier = Modifier.weight(0.6f)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Log set",
                tint = if (set.isLogged) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
