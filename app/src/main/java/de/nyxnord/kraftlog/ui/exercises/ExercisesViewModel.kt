package de.nyxnord.kraftlog.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.ExerciseCategory
import de.nyxnord.kraftlog.data.local.entity.MuscleGroup
import de.nyxnord.kraftlog.data.repository.ExerciseRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseRecords(
    val maxWeightKg: Float?,
    val bestEstimated1RM: Float?,
    val maxRepsInSet: Int?,
    val totalSessions: Int,
    val totalSets: Int,
    val totalVolumeKg: Float
)

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val recentSets: List<de.nyxnord.kraftlog.data.local.entity.WorkoutSet> = emptyList(),
    val records: ExerciseRecords? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisesViewModel(
    private val exerciseRepo: ExerciseRepository,
    private val workoutRepo: WorkoutRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val categoryFilter = MutableStateFlow<ExerciseCategory?>(null)

    val exercises: StateFlow<List<Exercise>> = combine(
        searchQuery, categoryFilter
    ) { query, category -> Pair(query, category) }
        .flatMapLatest { (query, category) ->
            when {
                query.isNotBlank() -> exerciseRepo.searchExercises(query)
                category != null -> exerciseRepo.getExercisesByCategory(category)
                else -> exerciseRepo.getAllExercises()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getDetailState(exerciseId: Long): StateFlow<ExerciseDetailUiState> =
        combine(
            exerciseRepo.getAllExercises(),
            workoutRepo.getSetsForExercise(exerciseId)
        ) { allExercises, sets ->
            val records = if (sets.isNotEmpty()) {
                val weightedSets = sets.filter { !it.isBodyweight && it.weightKg > 0 }
                ExerciseRecords(
                    maxWeightKg = weightedSets.maxOfOrNull { it.weightKg },
                    bestEstimated1RM = weightedSets.maxOfOrNull { it.weightKg * (1 + it.reps / 30f) },
                    maxRepsInSet = sets.maxOfOrNull { it.reps },
                    totalSessions = sets.map { it.sessionId }.distinct().size,
                    totalSets = sets.size,
                    totalVolumeKg = sets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()
                )
            } else null
            ExerciseDetailUiState(
                exercise = allExercises.find { it.id == exerciseId },
                recentSets = sets.take(50),
                records = records
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())

    fun saveCustomExercise(
        name: String,
        category: ExerciseCategory,
        primaryMuscles: List<MuscleGroup>,
        instructions: String
    ) {
        viewModelScope.launch {
            exerciseRepo.insertExercise(
                Exercise(
                    name = name,
                    category = category,
                    primaryMuscles = primaryMuscles,
                    instructions = instructions,
                    isCustom = true
                )
            )
        }
    }

    companion object {
        fun factory(exerciseRepo: ExerciseRepository, workoutRepo: WorkoutRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ExercisesViewModel(exerciseRepo, workoutRepo) as T
            }
    }
}
