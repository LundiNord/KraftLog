package de.nyxnord.kraftlog.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.data.local.entity.RunningEntry
import de.nyxnord.kraftlog.data.local.entity.SessionType
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.repository.AlternativeWorkoutRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class RunningUiState(
    val sessionId: Long = 0,
    val elapsedSeconds: Long = 0,
    val distanceKm: String = "",
    val manualHours: String = "",
    val manualMinutes: String = "",
    val manualSeconds: String = "",
    val isFinished: Boolean = false,
    val isDiscarded: Boolean = false,
    val isLoading: Boolean = true
)

/** Returns the manually entered duration in seconds, or null if all fields are blank. */
private fun RunningUiState.manualDurationSeconds(): Long? {
    val h = manualHours.toLongOrNull()
    val m = manualMinutes.toLongOrNull()
    val s = manualSeconds.toLongOrNull()
    if (h == null && m == null && s == null) return null
    return (h ?: 0) * 3600 + (m ?: 0) * 60 + (s ?: 0)
}

class RunningWorkoutViewModel(
    private val workoutRepo: WorkoutRepository,
    private val altRepo: AlternativeWorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunningUiState())
    val uiState: StateFlow<RunningUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val sessionId = workoutRepo.insertSession(
                WorkoutSession(name = "Running", sessionType = SessionType.RUNNING.name)
            )
            _uiState.update { it.copy(sessionId = sessionId, isLoading = false) }
            timerJob = launch {
                while (true) {
                    delay(1_000)
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    fun setDistance(value: String) {
        _uiState.update { it.copy(distanceKm = value) }
    }

    fun setManualHours(value: String) {
        _uiState.update { it.copy(manualHours = value) }
    }

    fun setManualMinutes(value: String) {
        _uiState.update { it.copy(manualMinutes = value) }
    }

    fun setManualSeconds(value: String) {
        _uiState.update { it.copy(manualSeconds = value) }
    }

    fun finishRun() {
        val state = _uiState.value
        val distKm = state.distanceKm.toFloatOrNull() ?: 0f
        val durationSecs = state.manualDurationSeconds() ?: state.elapsedSeconds
        timerJob?.cancel()
        viewModelScope.launch {
            altRepo.upsertRunningEntry(
                RunningEntry(sessionId = state.sessionId, distanceKm = distKm, durationSeconds = durationSecs)
            )
            workoutRepo.finishSession(state.sessionId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun discardRun() {
        timerJob?.cancel()
        viewModelScope.launch {
            workoutRepo.deleteSessionById(_uiState.value.sessionId)
            _uiState.update { it.copy(isDiscarded = true) }
        }
    }

    companion object {
        fun factory(workoutRepo: WorkoutRepository, altRepo: AlternativeWorkoutRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RunningWorkoutViewModel(workoutRepo, altRepo) as T
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningWorkoutScreen(
    app: KraftLogApplication,
    onFinished: (sessionId: Long) -> Unit,
    onDiscarded: () -> Unit
) {
    val vm: RunningWorkoutViewModel = viewModel(
        factory = RunningWorkoutViewModel.factory(app.workoutRepository, app.alternativeWorkoutRepository)
    )
    val state by vm.uiState.collectAsState()

    LaunchedEffect(state.isFinished) { if (state.isFinished) onFinished(state.sessionId) }
    LaunchedEffect(state.isDiscarded) { if (state.isDiscarded) onDiscarded() }

    // Effective duration: manual override if any field filled, else auto-timer
    val effectiveSecs: Long = run {
        val h = state.manualHours.toLongOrNull()
        val m = state.manualMinutes.toLongOrNull()
        val s = state.manualSeconds.toLongOrNull()
        if (h != null || m != null || s != null)
            (h ?: 0) * 3600 + (m ?: 0) * 60 + (s ?: 0)
        else
            state.elapsedSeconds
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Running")
                        Text(
                            formatElapsed(state.elapsedSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.discardRun() }) {
                        Icon(Icons.Default.Close, "Discard")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Distance input
                    OutlinedTextField(
                        value = state.distanceKm,
                        onValueChange = { vm.setDistance(it) },
                        label = { Text("Distance") },
                        suffix = { Text("km") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Duration override inputs
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Duration",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.manualHours,
                                onValueChange = { vm.setManualHours(it) },
                                label = { Text("h") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.manualMinutes,
                                onValueChange = { vm.setManualMinutes(it) },
                                label = { Text("min") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.manualSeconds,
                                onValueChange = { vm.setManualSeconds(it) },
                                label = { Text("sec") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        if (state.manualHours.isBlank() && state.manualMinutes.isBlank() && state.manualSeconds.isBlank()) {
                            Text(
                                "Leave blank to use the auto-timer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Live pace
                    val pace = paceText(state.distanceKm.toFloatOrNull(), effectiveSecs)
                    if (pace != null) {
                        Text(
                            "Pace: $pace /km",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { vm.finishRun() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finish Run")
            }
        }
    }
}

private fun paceText(distanceKm: Float?, elapsedSeconds: Long): String? {
    if (distanceKm == null || distanceKm <= 0f || elapsedSeconds <= 0) return null
    val paceSeconds = (elapsedSeconds / distanceKm).roundToInt()
    val m = paceSeconds / 60
    val s = paceSeconds % 60
    return "%d:%02d".format(m, s)
}
