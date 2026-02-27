package de.nyxnord.kraftlog.ui.history

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import de.nyxnord.kraftlog.ui.exercises.MuscleDiagram
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    app: KraftLogApplication,
    onSessionClick: (Long) -> Unit
) {
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(app.workoutRepository, app.exerciseRepository, app.routineRepository, app.reminderPreferences))
    val sessions by vm.sessions.collectAsState()
    val lifetimeStats by vm.lifetimeStats.collectAsState()
    val reminderEnabled by vm.reminderEnabled.collectAsState()
    val reminderIntervalDays by vm.reminderIntervalDays.collectAsState()
    val context = LocalContext.current
    var sessionToDelete by remember { mutableStateOf<WorkoutSession?>(null) }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Workout?") },
            text = { Text("This will permanently delete \"${sessionToDelete!!.name}\" and all its logged sets.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSession(sessionToDelete!!)
                    sessionToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Group by month-year
    val grouped = sessions.groupBy { session ->
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(session.startedAt))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("History") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                LifetimeStatsCard(lifetimeStats)
            }

            item {
                ReminderSettingsCard(
                    enabled = reminderEnabled,
                    intervalDays = reminderIntervalDays,
                    onEnabledChange = { vm.setReminderEnabled(context, it) },
                    onIntervalChange = { vm.setReminderIntervalDays(context, it) }
                )
            }

            if (sessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No workouts yet.\nComplete your first workout to see history.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                grouped.forEach { (month, monthlySessions) ->
                    item {
                        Text(
                            month,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(monthlySessions, key = { it.id }) { session ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    sessionToDelete = session
                                    false // snap back; deletion happens via dialog
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier.clip(MaterialTheme.shapes.medium),
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
                            SessionCard(session = session, onClick = { onSessionClick(session.id) })
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ReminderSettingsCard(
    enabled: Boolean,
    intervalDays: Int,
    onEnabledChange: (Boolean) -> Unit,
    onIntervalChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val hasPermission = remember(enabled) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onEnabledChange(true)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Workout Reminders", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Notify if no workout logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { turnOn ->
                        if (turnOn && !hasPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onEnabledChange(turnOn)
                        }
                    }
                )
            }

            if (enabled) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Remind after",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$intervalDays day${if (intervalDays == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = intervalDays.toFloat(),
                    onValueChange = { onIntervalChange(it.toInt()) },
                    valueRange = 1f..14f,
                    steps = 12
                )
            }
        }
    }
}

@Composable
private fun LifetimeStatsCard(stats: LifetimeStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Lifetime",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LifetimeStat("Workouts", "${stats.sessions}")
                LifetimeStat("Volume", "${"%.0f".format(stats.totalVolumeKg)} kg")
                LifetimeStat("Reps", "${stats.totalReps}")
            }
        }
    }
}

@Composable
private fun LifetimeStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SessionCard(session: WorkoutSession, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(session.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault())
                        .format(Date(session.startedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                session.finishedAt?.let { finished ->
                    val duration = finished - session.startedAt
                    Text(
                        formatDuration(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    app: KraftLogApplication,
    onBack: () -> Unit,
    onDeleted: () -> Unit = onBack,
    onRoutineCreated: (Long) -> Unit = {}
) {
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(app.workoutRepository, app.exerciseRepository, app.routineRepository, app.reminderPreferences))
    val sessionWithSets by vm.getSessionDetail(sessionId).collectAsState()
    val allExercises by vm.allExercises.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCreateRoutineDialog by remember { mutableStateOf(false) }
    var routineNameInput by remember { mutableStateOf("") }

    if (showCreateRoutineDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRoutineDialog = false },
            title = { Text("Save as Routine") },
            text = {
                OutlinedTextField(
                    value = routineNameInput,
                    onValueChange = { routineNameInput = it },
                    label = { Text("Routine name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sessionWithSets?.let { data ->
                            vm.createRoutineFromSession(routineNameInput, data.sets) { id ->
                                showCreateRoutineDialog = false
                                onRoutineCreated(id)
                            }
                        }
                    },
                    enabled = routineNameInput.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoutineDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Workout?") },
            text = { Text("This will permanently delete the workout and all its logged sets.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionWithSets?.session?.let { vm.deleteSession(it) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sessionWithSets?.session?.name ?: "Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete workout",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        val data = sessionWithSets
        if (data == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Loading…")
            }
            return@Scaffold
        }

        val session = data.session
        val setsByExercise = data.sets.groupBy { it.exerciseName }
        val totalVolume = data.sets.sumOf { (it.weightKg * it.reps).toDouble() }

        val exerciseMap = allExercises.associateBy { it.id }
        val workedExercises = data.sets.map { it.exerciseId }.distinct().mapNotNull { exerciseMap[it] }
        val workedPrimary = workedExercises.flatMap { it.primaryMuscles }.distinct()
        val workedSecondary = (workedExercises.flatMap { it.secondaryMuscles }.distinct() - workedPrimary.toSet()).toList()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault())
                            .format(Date(session.startedAt)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    session.finishedAt?.let { finished ->
                        Text(
                            formatDuration(finished - session.startedAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    "Total volume: ${"%.1f".format(totalVolume)} kg",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            item {
                OutlinedButton(
                    onClick = {
                        routineNameInput = session.name
                        showCreateRoutineDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save as Routine") }
            }

            if (workedPrimary.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Muscles Worked", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            MuscleDiagram(
                                primaryMuscles = workedPrimary,
                                secondaryMuscles = workedSecondary
                            )
                        }
                    }
                }
            }

            setsByExercise.forEach { (exerciseName, sets) ->
                item {
                    Text(
                        exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(sets.sortedBy { it.setNumber }, key = { it.id }) { set ->
                    SetDetailRow(set)
                }
                item { HorizontalDivider() }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SetDetailRow(set: WorkoutSet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Set ${set.setNumber}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (set.isBodyweight) "${set.reps} reps"
            else "${set.weightKg} kg × ${set.reps}",
            style = MaterialTheme.typography.bodyMedium
        )
        val vol = set.weightKg * set.reps
        Text("${"%.1f".format(vol)} kg", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    return if (minutes < 60) "${minutes}m" else "%dh %dm".format(minutes / 60, minutes % 60)
}
