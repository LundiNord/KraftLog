package de.nyxnord.kraftlog.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.Routine
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise
import de.nyxnord.kraftlog.data.local.relation.RoutineWithExerciseDetails
import de.nyxnord.kraftlog.data.repository.RoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesViewModel(private val routineRepo: RoutineRepository) : ViewModel() {

    val routines: StateFlow<List<RoutineWithExerciseDetails>> =
        routineRepo.getAllRoutinesWithExerciseDetails()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getRoutineDetail(id: Long): StateFlow<RoutineWithExerciseDetails?> =
        routineRepo.getRoutineWithExerciseDetails(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch { routineRepo.deleteRoutine(routine) }
    }

    suspend fun saveRoutine(
        routineId: Long,
        name: String,
        description: String,
        exercises: List<RoutineExercise>
    ): Long {
        return if (routineId == -1L) {
            val newId = routineRepo.insertRoutine(Routine(name = name, description = description))
            routineRepo.replaceRoutineExercises(newId, exercises.map { it.copy(routineId = newId) })
            newId
        } else {
            val existing = routineRepo.getRoutineById(routineId)!!
            routineRepo.updateRoutine(existing.copy(name = name, description = description))
            routineRepo.replaceRoutineExercises(routineId, exercises)
            routineId
        }
    }

    companion object {
        fun factory(routineRepo: RoutineRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RoutinesViewModel(routineRepo) as T
            }
    }
}
