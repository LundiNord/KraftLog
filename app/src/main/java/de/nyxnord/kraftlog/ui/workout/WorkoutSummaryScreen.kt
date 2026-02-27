package de.nyxnord.kraftlog.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import de.nyxnord.kraftlog.data.local.relation.WorkoutSessionWithSets
import de.nyxnord.kraftlog.data.repository.ExerciseRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import de.nyxnord.kraftlog.ui.exercises.MuscleDiagram
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class WorkoutSummaryViewModel(
    workoutRepo: WorkoutRepository,
    exerciseRepo: ExerciseRepository,
    sessionId: Long
) : ViewModel() {

    val sessionWithSets: StateFlow<WorkoutSessionWithSets?> =
        workoutRepo.getSessionWithSets(sessionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allExercises: StateFlow<List<Exercise>> =
        exerciseRepo.getAllExercises()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(workoutRepo: WorkoutRepository, exerciseRepo: ExerciseRepository, sessionId: Long) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    WorkoutSummaryViewModel(workoutRepo, exerciseRepo, sessionId) as T
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    sessionId: Long,
    app: KraftLogApplication,
    onDone: () -> Unit
) {
    val vm: WorkoutSummaryViewModel = viewModel(
        key = "summary_$sessionId",
        factory = WorkoutSummaryViewModel.factory(app.workoutRepository, app.exerciseRepository, sessionId)
    )
    val data by vm.sessionWithSets.collectAsState()
    val allExercises by vm.allExercises.collectAsState()

    var parties by remember { mutableStateOf<List<Party>>(emptyList()) }
    LaunchedEffect(Unit) {
        parties = listOf(
            Party(
                angle = 90,
                spread = 120,
                speed = 5f,
                maxSpeed = 20f,
                damping = 0.9f,
                timeToLive = 3_000L,
                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x59cdff),
                position = Position.Relative(0.5, 0.0),
                emitter = Emitter(duration = 400, TimeUnit.MILLISECONDS).max(200)
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Workout Complete") }) }
        ) { innerPadding ->
            val session = data?.session
            val sets = data?.sets ?: emptyList()
            val setsByExercise = sets.groupBy { it.exerciseName }
            val totalVolume = sets.sumOf { (it.weightKg * it.reps).toDouble() }

            val exerciseMap = allExercises.associateBy { it.id }
            val workedExercises = sets.map { it.exerciseId }.distinct().mapNotNull { exerciseMap[it] }
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
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            session?.name ?: "",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        session?.startedAt?.let { start ->
                            Text(
                                SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault())
                                    .format(Date(start)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        session?.finishedAt?.let { finished ->
                            SummaryStatCard(
                                label = "Duration",
                                value = formatSummaryDuration(finished - session.startedAt),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        SummaryStatCard(
                            label = "Exercises",
                            value = "${setsByExercise.size}",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatCard(
                            label = "Sets",
                            value = "${sets.size}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryStatCard(
                            label = "Total Reps",
                            value = "${sets.sumOf { it.reps }}",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatCard(
                            label = "Total Volume",
                            value = "${"%.1f".format(totalVolume)} kg",
                            modifier = Modifier.weight(1f)
                        )
                    }
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

                setsByExercise.forEach { (exerciseName, exerciseSets) ->
                    item {
                        Text(
                            exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(exerciseSets.sortedBy { it.setNumber }, key = { it.id }) { set ->
                        SummarySetRow(set)
                    }
                    item { HorizontalDivider() }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }

        if (parties.isNotEmpty()) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = parties
            )
        }
    }
}

@Composable
private fun SummaryStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SummarySetRow(set: WorkoutSet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Set ${set.setNumber}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (set.isBodyweight) "${set.reps} reps"
            else "${set.weightKg.let { if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString() }} kg × ${set.reps}",
            style = MaterialTheme.typography.bodyMedium
        )
        val vol = set.weightKg * set.reps
        Text(
            "${"%.1f".format(vol)} kg",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatSummaryDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    return if (minutes < 60) "${minutes}m" else "%dh %dm".format(minutes / 60, minutes % 60)
}
