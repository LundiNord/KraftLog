package de.nyxnord.kraftlog.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import de.nyxnord.kraftlog.data.repository.ExerciseRepository
import de.nyxnord.kraftlog.data.repository.RoutineRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveSet(
    val id: Long = 0,
    val setNumber: Int,
    val reps: String = "",
    val weight: String = "",
    val isBodyweight: Boolean = false,
    val isLogged: Boolean = false
)

data class LiveExercise(
    val exerciseId: Long,
    val exerciseName: String,
    val sets: List<LiveSet> = emptyList(),
    val restSeconds: Int = 60
)

data class ActiveWorkoutUiState(
    val sessionId: Long = 0,
    val sessionName: String = "",
    val elapsedSeconds: Long = 0,
    val exercises: List<LiveExercise> = emptyList(),
    val isFinished: Boolean = false,
    val isLoading: Boolean = true,
    val restTimerSeconds: Int? = null
)

class ActiveWorkoutViewModel(
    private val routineId: Long,
    private val routineRepo: RoutineRepository,
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var restTimerJob: Job? = null

    init {
        viewModelScope.launch {
            launch { runTimer() }
            initSession()
        }
    }

    private suspend fun initSession() {
        val sessionName: String
        val exercises: List<LiveExercise>

        if (routineId != -1L) {
            val detail = routineRepo.getRoutineWithExerciseDetails(routineId).first()
            sessionName = detail?.routine?.name ?: "Workout"
            exercises = detail?.exerciseDetails
                ?.sortedBy { it.routineExercise.orderIndex }
                ?.map { item ->
                    LiveExercise(
                        exerciseId = item.exercise.id,
                        exerciseName = item.exercise.name,
                        restSeconds = item.routineExercise.restSeconds,
                        sets = (1..item.routineExercise.targetSets).map { setNum ->
                            LiveSet(
                                setNumber = setNum,
                                reps = item.routineExercise.targetReps.toString(),
                                weight = item.routineExercise.targetWeightKg?.toString() ?: ""
                            )
                        }
                    )
                } ?: emptyList()
        } else {
            sessionName = "Ad-hoc Workout"
            exercises = emptyList()
        }

        val sessionId = workoutRepo.insertSession(
            WorkoutSession(
                routineId = if (routineId == -1L) null else routineId,
                name = sessionName
            )
        )

        _uiState.update {
            it.copy(
                sessionId = sessionId,
                sessionName = sessionName,
                exercises = exercises,
                isLoading = false
            )
        }
    }

    private suspend fun runTimer() {
        while (true) {
            delay(1_000)
            _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
        }
    }

    fun logSet(exerciseIndex: Int, setIndex: Int) {
        val state = _uiState.value
        val ex = state.exercises[exerciseIndex]
        val set = ex.sets[setIndex]
        val reps = set.reps.toIntOrNull() ?: 0
        val weight = set.weight.toFloatOrNull() ?: 0f

        viewModelScope.launch {
            workoutRepo.insertSet(
                WorkoutSet(
                    sessionId = state.sessionId,
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.exerciseName,
                    setNumber = set.setNumber,
                    reps = reps,
                    weightKg = weight,
                    isBodyweight = set.isBodyweight
                )
            )
        }

        val updatedExercises = state.exercises.toMutableList()
        val updatedSets = ex.sets.toMutableList()
        updatedSets[setIndex] = set.copy(isLogged = true)
        updatedExercises[exerciseIndex] = ex.copy(sets = updatedSets)
        _uiState.update { it.copy(exercises = updatedExercises) }

        val restSecs = ex.restSeconds
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            for (remaining in restSecs downTo 0) {
                _uiState.update { it.copy(restTimerSeconds = remaining) }
                if (remaining > 0) delay(1_000)
            }
            _uiState.update { it.copy(restTimerSeconds = null) }
        }
    }

    fun dismissRestTimer() {
        restTimerJob?.cancel()
        _uiState.update { it.copy(restTimerSeconds = null) }
    }

    fun updateSetField(exerciseIndex: Int, setIndex: Int, reps: String? = null, weight: String? = null) {
        val state = _uiState.value
        val updatedExercises = state.exercises.toMutableList()
        val ex = updatedExercises[exerciseIndex]
        val updatedSets = ex.sets.toMutableList()
        val set = updatedSets[setIndex]
        updatedSets[setIndex] = set.copy(
            reps = reps ?: set.reps,
            weight = weight ?: set.weight
        )
        updatedExercises[exerciseIndex] = ex.copy(sets = updatedSets)
        _uiState.update { it.copy(exercises = updatedExercises) }
    }

    fun addSet(exerciseIndex: Int) {
        val state = _uiState.value
        val updatedExercises = state.exercises.toMutableList()
        val ex = updatedExercises[exerciseIndex]
        val nextSetNum = (ex.sets.lastOrNull()?.setNumber ?: 0) + 1
        val updatedSets = ex.sets.toMutableList()
        updatedSets.add(LiveSet(setNumber = nextSetNum))
        updatedExercises[exerciseIndex] = ex.copy(sets = updatedSets)
        _uiState.update { it.copy(exercises = updatedExercises) }
    }

    fun addExercise(exerciseId: Long, exerciseName: String) {
        val state = _uiState.value
        val updatedExercises = state.exercises.toMutableList()
        updatedExercises.add(
            LiveExercise(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                sets = listOf(LiveSet(setNumber = 1))
            )
        )
        _uiState.update { it.copy(exercises = updatedExercises) }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            workoutRepo.finishSession(_uiState.value.sessionId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    companion object {
        fun factory(
            routineId: Long,
            routineRepo: RoutineRepository,
            workoutRepo: WorkoutRepository,
            exerciseRepo: ExerciseRepository
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ActiveWorkoutViewModel(routineId, routineRepo, workoutRepo, exerciseRepo) as T
        }
    }
}
