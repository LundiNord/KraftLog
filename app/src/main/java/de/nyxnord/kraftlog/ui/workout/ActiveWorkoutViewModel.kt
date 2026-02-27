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
    val restSeconds: Int = 60,
    val lastSets: List<de.nyxnord.kraftlog.data.local.entity.WorkoutSet> = emptyList()
)

data class ActiveWorkoutUiState(
    val sessionId: Long = 0,
    val sessionName: String = "",
    val elapsedSeconds: Long = 0,
    val exercises: List<LiveExercise> = emptyList(),
    val isFinished: Boolean = false,
    val isDiscarded: Boolean = false,
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
        // Restore an existing unfinished session if one exists
        val existingSession = workoutRepo.getActiveSessionSync()
        if (existingSession != null) {
            restoreSession(existingSession)
            return
        }

        // No existing session — start a new one
        val sessionName: String
        val rawExerciseDetails: List<de.nyxnord.kraftlog.data.local.relation.RoutineExerciseWithExercise>?

        if (routineId != -1L) {
            val detail = routineRepo.getRoutineWithExerciseDetails(routineId).first()
            sessionName = detail?.routine?.name ?: "Workout"
            rawExerciseDetails = detail?.exerciseDetails?.sortedBy { it.routineExercise.orderIndex }
        } else {
            sessionName = "Ad-hoc Workout"
            rawExerciseDetails = null
        }

        val sessionId = workoutRepo.insertSession(
            WorkoutSession(
                routineId = if (routineId == -1L) null else routineId,
                name = sessionName
            )
        )

        val exercises = rawExerciseDetails?.map { item ->
            val lastSets = workoutRepo.getLastSessionSetsForExercise(item.exercise.id, sessionId)
            val re = item.routineExercise
            val perSetWeights = re.targetWeightsPerSet.split(",").filter { it.isNotBlank() }
            val perSetReps = re.targetRepsPerSet.split(",").filter { it.isNotBlank() }
            LiveExercise(
                exerciseId = item.exercise.id,
                exerciseName = item.exercise.name,
                restSeconds = re.restSeconds,
                sets = (1..re.targetSets).map { setNum ->
                    LiveSet(
                        setNumber = setNum,
                        reps = perSetReps.getOrElse(setNum - 1) { re.targetReps.toString() },
                        weight = perSetWeights.getOrElse(setNum - 1) { re.targetWeightKg?.toString() ?: "" }
                    )
                },
                lastSets = lastSets
            )
        } ?: emptyList()

        _uiState.update {
            it.copy(
                sessionId = sessionId,
                sessionName = sessionName,
                exercises = exercises,
                isLoading = false
            )
        }
    }

    private suspend fun restoreSession(session: WorkoutSession) {
        val sets = workoutRepo.getSetsForSessionList(session.id)
        val exerciseMap = LinkedHashMap<Long, MutableList<WorkoutSet>>()
        for (set in sets) {
            exerciseMap.getOrPut(set.exerciseId) { mutableListOf() }.add(set)
        }

        val exercises = exerciseMap.entries.map { (exerciseId, exerciseSets) ->
            val lastSets = workoutRepo.getLastSessionSetsForExercise(exerciseId, session.id)
            LiveExercise(
                exerciseId = exerciseId,
                exerciseName = exerciseSets.first().exerciseName,
                sets = exerciseSets.map { set ->
                    val weightStr = if (set.isBodyweight) ""
                    else if (set.weightKg == set.weightKg.toLong().toFloat())
                        set.weightKg.toLong().toString()
                    else set.weightKg.toString()
                    LiveSet(
                        id = set.id,
                        setNumber = set.setNumber,
                        reps = set.reps.toString(),
                        weight = weightStr,
                        isBodyweight = set.isBodyweight,
                        isLogged = true
                    )
                },
                lastSets = lastSets
            )
        }

        _uiState.update {
            it.copy(
                sessionId = session.id,
                sessionName = session.name,
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

        // Immediately mark as logged in the UI
        val updatedExercises = state.exercises.toMutableList()
        val updatedSets = ex.sets.toMutableList()
        updatedSets[setIndex] = set.copy(isLogged = true)
        updatedExercises[exerciseIndex] = ex.copy(sets = updatedSets)
        _uiState.update { it.copy(exercises = updatedExercises) }

        // Persist and store the returned row ID so edits can update the record
        viewModelScope.launch {
            val insertedId = workoutRepo.insertSet(
                WorkoutSet(
                    id = set.id,
                    sessionId = state.sessionId,
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.exerciseName,
                    setNumber = set.setNumber,
                    reps = reps,
                    weightKg = weight,
                    isBodyweight = set.isBodyweight
                )
            )
            _uiState.update { s ->
                val exList = s.exercises.toMutableList()
                val exItem = exList[exerciseIndex]
                val setList = exItem.sets.toMutableList()
                setList[setIndex] = setList[setIndex].copy(id = insertedId)
                exList[exerciseIndex] = exItem.copy(sets = setList)
                s.copy(exercises = exList)
            }
        }

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

    fun unlogSet(exerciseIndex: Int, setIndex: Int) {
        _uiState.update { state ->
            val exList = state.exercises.toMutableList()
            val ex = exList[exerciseIndex]
            val setList = ex.sets.toMutableList()
            setList[setIndex] = setList[setIndex].copy(isLogged = false)
            exList[exerciseIndex] = ex.copy(sets = setList)
            state.copy(exercises = exList)
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
        viewModelScope.launch {
            val lastSets = workoutRepo.getLastSessionSetsForExercise(exerciseId, _uiState.value.sessionId)
            val state = _uiState.value
            val updatedExercises = state.exercises.toMutableList()
            updatedExercises.add(
                LiveExercise(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    sets = listOf(LiveSet(setNumber = 1)),
                    lastSets = lastSets
                )
            )
            _uiState.update { it.copy(exercises = updatedExercises) }
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            workoutRepo.finishSession(_uiState.value.sessionId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun discardWorkout() {
        viewModelScope.launch {
            workoutRepo.deleteSessionById(_uiState.value.sessionId)
            _uiState.update { it.copy(isDiscarded = true) }
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
