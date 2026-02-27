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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.nyxnord.kraftlog.KraftLogApplication
import de.nyxnord.kraftlog.data.local.entity.BoulderingRoute
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

data class BoulderingUiState(
    val sessionId: Long = 0,
    val elapsedSeconds: Long = 0,
    val routes: List<BoulderingRoute> = emptyList(),
    val isFinished: Boolean = false,
    val isDiscarded: Boolean = false,
    val isLoading: Boolean = true
)

class BoulderingWorkoutViewModel(
    private val workoutRepo: WorkoutRepository,
    private val altRepo: AlternativeWorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoulderingUiState())
    val uiState: StateFlow<BoulderingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val sessionId = workoutRepo.insertSession(
                WorkoutSession(name = "Bouldering", sessionType = SessionType.BOULDERING.name)
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

    fun logRoute(description: String, isCompleted: Boolean) {
        val sessionId = _uiState.value.sessionId
        viewModelScope.launch {
            val id = altRepo.insertBoulderingRoute(
                BoulderingRoute(sessionId = sessionId, description = description.trim(), isCompleted = isCompleted)
            )
            val route = BoulderingRoute(id = id, sessionId = sessionId, description = description.trim(), isCompleted = isCompleted)
            _uiState.update { it.copy(routes = it.routes + route) }
        }
    }

    fun removeRoute(route: BoulderingRoute) {
        viewModelScope.launch {
            altRepo.deleteBoulderingRoute(route)
            _uiState.update { it.copy(routes = it.routes - route) }
        }
    }

    fun finishSession() {
        timerJob?.cancel()
        viewModelScope.launch {
            workoutRepo.finishSession(_uiState.value.sessionId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun discardSession() {
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
                    BoulderingWorkoutViewModel(workoutRepo, altRepo) as T
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoulderingWorkoutScreen(
    app: KraftLogApplication,
    onFinished: (sessionId: Long) -> Unit,
    onDiscarded: () -> Unit
) {
    val vm: BoulderingWorkoutViewModel = viewModel(
        factory = BoulderingWorkoutViewModel.factory(app.workoutRepository, app.alternativeWorkoutRepository)
    )
    val state by vm.uiState.collectAsState()
    var descriptionInput by remember { mutableStateOf("") }

    LaunchedEffect(state.isFinished) { if (state.isFinished) onFinished(state.sessionId) }
    LaunchedEffect(state.isDiscarded) { if (state.isDiscarded) onDiscarded() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bouldering")
                        Text(
                            formatElapsed(state.elapsedSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.discardSession() }) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Route log input card
            item {
                Spacer(Modifier.height(4.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Log a Route", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = descriptionInput,
                            onValueChange = { descriptionInput = it },
                            label = { Text("Description") },
                            placeholder = { Text("e.g. Red 6b, V3, Overhang…") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (descriptionInput.isNotBlank()) {
                                        vm.logRoute(descriptionInput, isCompleted = true)
                                        descriptionInput = ""
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = descriptionInput.isNotBlank()
                            ) {
                                Icon(Icons.Default.Check, null)
                                Text("Completed", modifier = Modifier.padding(start = 4.dp))
                            }
                            OutlinedButton(
                                onClick = {
                                    if (descriptionInput.isNotBlank()) {
                                        vm.logRoute(descriptionInput, isCompleted = false)
                                        descriptionInput = ""
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = descriptionInput.isNotBlank()
                            ) {
                                Text("Attempted")
                            }
                        }
                    }
                }
            }

            // Logged routes
            if (state.routes.isNotEmpty()) {
                item {
                    Text(
                        "Routes (${state.routes.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(state.routes, key = { it.id }) { route ->
                    RouteRow(route = route, onRemove = { vm.removeRoute(route) })
                    HorizontalDivider()
                }
            }

            // Summary counts
            if (state.routes.isNotEmpty()) {
                item {
                    val completed = state.routes.count { it.isCompleted }
                    val attempted = state.routes.count { !it.isCompleted }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("$completed", style = MaterialTheme.typography.titleLarge)
                                Text("Completed", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Card(modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("$attempted", style = MaterialTheme.typography.titleLarge)
                                Text("Attempted", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Button(
                    onClick = { vm.finishSession() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finish Session")
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun RouteRow(route: BoulderingRoute, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(route.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            if (route.isCompleted) "Completed" else "Attempted",
            style = MaterialTheme.typography.bodySmall,
            color = if (route.isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
