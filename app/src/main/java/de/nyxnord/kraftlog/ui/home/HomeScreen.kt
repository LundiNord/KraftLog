package de.nyxnord.kraftlog.ui.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.data.local.relation.RoutineWithExerciseDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: KraftLogApplication,
    onStartWorkout: (routineId: Long) -> Unit,
    onStartRunning: () -> Unit,
    onStartBouldering: () -> Unit
) {
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(app.routineRepository, app.workoutRepository)
    )
    val state by vm.uiState.collectAsState()
    var showTypePicker by remember { mutableStateOf(false) }

    if (showTypePicker) {
        ModalBottomSheet(
            onDismissRequest = { showTypePicker = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Text(
                "Start a Workout",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Strength") },
                supportingContent = { Text("Track sets, reps and weight") },
                leadingContent = { Icon(Icons.Default.FitnessCenter, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    showTypePicker = false
                    onStartWorkout(-1L)
                }
            )
            ListItem(
                headlineContent = { Text("Running") },
                supportingContent = { Text("Log distance and pace") },
                leadingContent = { Icon(Icons.Default.DirectionsRun, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    showTypePicker = false
                    onStartRunning()
                }
            )
            ListItem(
                headlineContent = { Text("Bouldering") },
                supportingContent = { Text("Log routes by grade") },
                leadingContent = { Icon(Icons.Default.Terrain, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    showTypePicker = false
                    onStartBouldering()
                }
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("KraftLog") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTypePicker = true },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                text = { Text("Quick Workout") }
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
                Text(
                    text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Session count stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "This Week",
                        value = "${state.weeklySessions}",
                        unit = "sessions",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "This Month",
                        value = "${state.monthlySessions}",
                        unit = "sessions",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "This Year",
                        value = "${state.yearlySessions}",
                        unit = "sessions",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Active session banner (STRENGTH only — others start fresh)
            state.activeSession?.let { active ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartWorkout(active.routineId ?: -1L) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Active Workout", style = MaterialTheme.typography.labelMedium)
                                Text(active.name, style = MaterialTheme.typography.titleMedium)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                        }
                    }
                }
            }

            if (state.routines.isNotEmpty()) {
                item {
                    Text(
                        "Start a Routine",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.routines) { routineWithDetails ->
                    RoutineQuickStartCard(
                        routineWithDetails = routineWithDetails,
                        onStart = { onStartWorkout(routineWithDetails.routine.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(unit, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RoutineQuickStartCard(
    routineWithDetails: RoutineWithExerciseDetails,
    onStart: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(routineWithDetails.routine.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${routineWithDetails.exerciseDetails.size} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onStart) {
                Text("Start")
            }
        }
    }
}
