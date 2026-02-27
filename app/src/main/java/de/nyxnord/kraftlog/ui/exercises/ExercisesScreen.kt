package de.nyxnord.kraftlog.ui.exercises

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val detailState by vm.getDetailState(exerciseId).collectAsState()
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
                    Text("Primary Muscles", style = MaterialTheme.typography.titleSmall)
                    Text(ex.primaryMuscles.joinToString(", ") {
                        it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                    })
                }
                if (ex.secondaryMuscles.isNotEmpty()) {
                    item {
                        Text("Secondary Muscles", style = MaterialTheme.typography.titleSmall)
                        Text(ex.secondaryMuscles.joinToString(", ") {
                            it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                        })
                    }
                }
                if (ex.instructions.isNotBlank()) {
                    item {
                        Text("Instructions", style = MaterialTheme.typography.titleSmall)
                        Text(ex.instructions)
                    }
                }
            }

            if (detailState.recentSets.isNotEmpty()) {
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
