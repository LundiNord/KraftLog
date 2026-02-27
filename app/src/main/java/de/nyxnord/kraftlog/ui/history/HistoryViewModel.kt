package de.nyxnord.kraftlog.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.BoulderingRoute
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.Routine
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise
import de.nyxnord.kraftlog.data.local.entity.RunningEntry
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import de.nyxnord.kraftlog.data.local.relation.WorkoutSessionWithSets
import de.nyxnord.kraftlog.data.preferences.ReminderPreferences
import de.nyxnord.kraftlog.data.repository.AlternativeWorkoutRepository
import de.nyxnord.kraftlog.data.repository.ExerciseRepository
import de.nyxnord.kraftlog.data.repository.RoutineRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import de.nyxnord.kraftlog.notification.ReminderScheduler
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LifetimeStats(val sessions: Int, val totalVolumeKg: Double, val totalReps: Int)

class HistoryViewModel(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
    private val routineRepo: RoutineRepository,
    private val reminderPreferences: ReminderPreferences,
    private val altRepo: AlternativeWorkoutRepository
) : ViewModel() {

    private val _reminderEnabled = MutableStateFlow(reminderPreferences.enabled)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderIntervalDays = MutableStateFlow(reminderPreferences.intervalDays)
    val reminderIntervalDays: StateFlow<Int> = _reminderIntervalDays.asStateFlow()

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        reminderPreferences.enabled = enabled
        _reminderEnabled.value = enabled
        if (enabled) ReminderScheduler.schedule(context)
        else ReminderScheduler.cancel(context)
    }

    fun setReminderIntervalDays(context: Context, days: Int) {
        reminderPreferences.intervalDays = days
        _reminderIntervalDays.value = days
        if (reminderPreferences.enabled) ReminderScheduler.reschedule(context)
    }

    val sessions: StateFlow<List<WorkoutSession>> =
        workoutRepo.getAllSessions()
            .map { list -> list.filter { it.finishedAt != null } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allExercises: StateFlow<List<Exercise>> =
        exerciseRepo.getAllExercises()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lifetimeStats: StateFlow<LifetimeStats> = combine(
        workoutRepo.getAllSessions().map { list -> list.filter { it.finishedAt != null } },
        workoutRepo.getAllSets()
    ) { finishedSessions, sets ->
        LifetimeStats(
            sessions = finishedSessions.size,
            totalVolumeKg = sets.sumOf { (it.weightKg * it.reps).toDouble() },
            totalReps = sets.sumOf { it.reps }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LifetimeStats(0, 0.0, 0))

    // Cached per session ID so recompositions don't recreate flows starting from null
    private val sessionDetailCache = HashMap<Long, StateFlow<WorkoutSessionWithSets?>>()
    fun getSessionDetail(id: Long): StateFlow<WorkoutSessionWithSets?> =
        sessionDetailCache.getOrPut(id) {
            workoutRepo.getSessionWithSets(id)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    private val runningEntryCache = HashMap<Long, StateFlow<RunningEntry?>>()
    fun getRunningEntry(sessionId: Long): StateFlow<RunningEntry?> =
        runningEntryCache.getOrPut(sessionId) {
            altRepo.getRunningEntry(sessionId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    private val boulderingRoutesCache = HashMap<Long, StateFlow<List<BoulderingRoute>>>()
    fun getBoulderingRoutes(sessionId: Long): StateFlow<List<BoulderingRoute>> =
        boulderingRoutesCache.getOrPut(sessionId) {
            altRepo.getBoulderingRoutes(sessionId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch { workoutRepo.deleteSession(session) }
    }

    fun createRoutineFromSession(name: String, sets: List<WorkoutSet>, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val routineId = routineRepo.insertRoutine(Routine(name = name))
            val uniqueExerciseIds = sets.map { it.exerciseId }.distinct()
            val routineExercises = uniqueExerciseIds.mapIndexed { idx, exerciseId ->
                val exerciseSets = sets.filter { it.exerciseId == exerciseId }
                RoutineExercise(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    orderIndex = idx,
                    targetSets = exerciseSets.size,
                    targetReps = exerciseSets.firstOrNull()?.reps ?: 10,
                    targetWeightKg = if (exerciseSets.all { it.isBodyweight }) null
                                     else exerciseSets.filter { !it.isBodyweight }.maxOfOrNull { it.weightKg }
                )
            }
            routineRepo.replaceRoutineExercises(routineId, routineExercises)
            onCreated(routineId)
        }
    }

    companion object {
        fun factory(
            workoutRepo: WorkoutRepository,
            exerciseRepo: ExerciseRepository,
            routineRepo: RoutineRepository,
            reminderPreferences: ReminderPreferences,
            altRepo: AlternativeWorkoutRepository
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HistoryViewModel(workoutRepo, exerciseRepo, routineRepo, reminderPreferences, altRepo) as T
        }
    }
}
