package de.nyxnord.kraftlog.ui.friends

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.ExerciseShareRecord
import de.nyxnord.kraftlog.data.SessionShareRecord
import de.nyxnord.kraftlog.data.ShareableStats
import de.nyxnord.kraftlog.data.bluetooth.BluetoothShareService
import de.nyxnord.kraftlog.data.preferences.UserPreferences
import de.nyxnord.kraftlog.data.repository.ExerciseRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class FriendsUiState {
    object Idle : FriendsUiState()
    object Waiting : FriendsUiState()
    object Scanning : FriendsUiState()
    data class DevicesFound(val devices: List<BluetoothDevice>) : FriendsUiState()
    object Connecting : FriendsUiState()
    data class Connected(
        val myStats: ShareableStats,
        val friendStats: ShareableStats
    ) : FriendsUiState()
    data class Error(val message: String) : FriendsUiState()
}

class FriendsViewModel(
    private val userPreferences: UserPreferences,
    private val exerciseRepo: ExerciseRepository,
    private val workoutRepo: WorkoutRepository,
    bluetoothAdapter: BluetoothAdapter?
) : ViewModel() {

    private val _uiState = MutableStateFlow<FriendsUiState>(FriendsUiState.Idle)
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    val displayName = MutableStateFlow(userPreferences.displayName)

    private val service = bluetoothAdapter?.let { BluetoothShareService(it) }
    private val discoveredDevices = mutableListOf<BluetoothDevice>()

    fun saveDisplayName(name: String) {
        userPreferences.displayName = name
        displayName.value = name
    }

    fun onDeviceFound(device: BluetoothDevice) {
        if (discoveredDevices.none { it.address == device.address }) {
            discoveredDevices.add(device)
        }
        _uiState.value = FriendsUiState.DevicesFound(discoveredDevices.toList())
    }

    fun startScanning() {
        discoveredDevices.clear()
        _uiState.value = FriendsUiState.Scanning
    }

    fun startWaiting() {
        _uiState.value = FriendsUiState.Waiting
        viewModelScope.launch {
            runCatching {
                val myStats = buildMyStats()
                val friendStats = service?.startServer(myStats)
                    ?: error("Bluetooth not available on this device")
                Pair(myStats, friendStats)
            }.onSuccess { (myStats, friendStats) ->
                _uiState.value = FriendsUiState.Connected(myStats, friendStats)
            }.onFailure { e ->
                // Guard against overwriting Idle state set by reset()
                if (_uiState.value is FriendsUiState.Waiting) {
                    _uiState.value = FriendsUiState.Error(e.message ?: "Connection failed")
                }
            }
        }
    }

    fun connectTo(device: BluetoothDevice) {
        _uiState.value = FriendsUiState.Connecting
        viewModelScope.launch {
            runCatching {
                val myStats = buildMyStats()
                val friendStats = service?.connectToDevice(device, myStats)
                    ?: error("Bluetooth not available on this device")
                Pair(myStats, friendStats)
            }.onSuccess { (myStats, friendStats) ->
                _uiState.value = FriendsUiState.Connected(myStats, friendStats)
            }.onFailure { e ->
                if (_uiState.value is FriendsUiState.Connecting) {
                    _uiState.value = FriendsUiState.Error(e.message ?: "Connection failed")
                }
            }
        }
    }

    fun reset() {
        service?.cancel()
        discoveredDevices.clear()
        _uiState.value = FriendsUiState.Idle
    }

    private suspend fun buildMyStats(): ShareableStats {
        val allSets = workoutRepo.getAllSets().first()
        val allExercises = exerciseRepo.getAllExercises().first()
        val exerciseMap = allExercises.associateBy { it.id }

        val exerciseRecords = allSets
            .groupBy { it.exerciseId }
            .mapNotNull { (exerciseId, sets) ->
                val exercise = exerciseMap[exerciseId] ?: return@mapNotNull null
                val weightedSets = sets.filter { !it.isBodyweight && it.weightKg > 0 }
                ExerciseShareRecord(
                    exerciseName = exercise.name,
                    maxWeightKg = weightedSets.maxOfOrNull { it.weightKg },
                    bestEstimated1RM = weightedSets.maxOfOrNull { it.weightKg * (1 + it.reps / 30f) },
                    maxRepsInSet = sets.maxOfOrNull { it.reps },
                    totalSessions = sets.map { it.sessionId }.distinct().size,
                    totalVolumeKg = sets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()
                )
            }

        val sessions = workoutRepo.getFinishedSessionsList().takeLast(10)
        val recentSessions = sessions.map { session ->
            val sessionSets = workoutRepo.getSetsForSessionList(session.id)
            SessionShareRecord(
                name = session.name,
                date = session.startedAt,
                totalVolumeKg = sessionSets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat(),
                exerciseNames = sessionSets.map { it.exerciseName }.distinct()
            )
        }

        return ShareableStats(
            displayName = userPreferences.displayName.ifBlank { "Friend" },
            exerciseRecords = exerciseRecords,
            recentSessions = recentSessions
        )
    }

    companion object {
        fun factory(
            userPreferences: UserPreferences,
            exerciseRepo: ExerciseRepository,
            workoutRepo: WorkoutRepository,
            bluetoothAdapter: BluetoothAdapter?
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FriendsViewModel(userPreferences, exerciseRepo, workoutRepo, bluetoothAdapter) as T
        }
    }
}
