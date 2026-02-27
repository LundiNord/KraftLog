package de.nyxnord.kraftlog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.Routine
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import de.nyxnord.kraftlog.data.local.relation.WorkoutSessionWithSets
import de.nyxnord.kraftlog.data.repository.ExerciseRepository
import de.nyxnord.kraftlog.data.repository.RoutineRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
    private val routineRepo: RoutineRepository
) : ViewModel() {

    val sessions: StateFlow<List<WorkoutSession>> =
        workoutRepo.getAllSessions()
            .map { list -> list.filter { it.finishedAt != null } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allExercises: StateFlow<List<Exercise>> =
        exerciseRepo.getAllExercises()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getSessionDetail(id: Long): StateFlow<WorkoutSessionWithSets?> =
        workoutRepo.getSessionWithSets(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
        fun factory(workoutRepo: WorkoutRepository, exerciseRepo: ExerciseRepository, routineRepo: RoutineRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(workoutRepo, exerciseRepo, routineRepo) as T
            }
    }
}
