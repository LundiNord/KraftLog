package de.nyxnord.kraftlog.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.ExerciseCategory
import de.nyxnord.kraftlog.data.local.entity.Routine
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise
import de.nyxnord.kraftlog.data.local.relation.RoutineWithExerciseDetails
import de.nyxnord.kraftlog.data.repository.ExerciseRepository
import de.nyxnord.kraftlog.data.repository.RoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class RoutinesViewModel(private val routineRepo: RoutineRepository) : ViewModel() {

    val routines: StateFlow<List<RoutineWithExerciseDetails>> =
        routineRepo.getAllRoutinesWithExerciseDetails()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getRoutineDetail(id: Long): StateFlow<RoutineWithExerciseDetails?> =
        routineRepo.getRoutineWithExerciseDetails(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun loadRoutineForEdit(id: Long): RoutineWithExerciseDetails? =
        routineRepo.getRoutineWithExerciseDetails(id).first()

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

    suspend fun exportRoutineJson(id: Long): String {
        val detail = routineRepo.getRoutineWithExerciseDetails(id).first() ?: return ""
        val exercises = JSONArray()
        detail.exerciseDetails.sortedBy { it.routineExercise.orderIndex }.forEach { item ->
            val re = item.routineExercise
            exercises.put(JSONObject().apply {
                put("exerciseName", item.exercise.name)
                put("orderIndex", re.orderIndex)
                put("targetSets", re.targetSets)
                put("targetReps", re.targetReps)
                if (re.targetWeightKg != null) put("targetWeightKg", re.targetWeightKg.toDouble())
                else put("targetWeightKg", JSONObject.NULL)
                put("targetWeightsPerSet", re.targetWeightsPerSet)
                put("restSeconds", re.restSeconds)
                put("notes", re.notes)
            })
        }
        return JSONObject().apply {
            put("version", 1)
            put("name", detail.routine.name)
            put("description", detail.routine.description)
            put("exercises", exercises)
        }.toString(2)
    }

    suspend fun importRoutineFromJson(json: String, exerciseRepo: ExerciseRepository): Boolean {
        return try {
            val obj = JSONObject(json)
            val name = obj.getString("name")
            val description = obj.optString("description", "")
            val exArray = obj.getJSONArray("exercises")
            val newRoutineId = routineRepo.insertRoutine(Routine(name = name, description = description))
            val routineExercises = (0 until exArray.length()).map { i ->
                val ex = exArray.getJSONObject(i)
                val exerciseName = ex.getString("exerciseName")
                val exercise = exerciseRepo.getByName(exerciseName) ?: run {
                    val newId = exerciseRepo.insertExercise(
                        Exercise(
                            name = exerciseName,
                            category = ExerciseCategory.STRENGTH,
                            primaryMuscles = emptyList(),
                            isCustom = true
                        )
                    )
                    exerciseRepo.getExerciseById(newId)!!
                }
                RoutineExercise(
                    routineId = newRoutineId,
                    exerciseId = exercise.id,
                    orderIndex = ex.optInt("orderIndex", i),
                    targetSets = ex.optInt("targetSets", 3),
                    targetReps = ex.optInt("targetReps", 10),
                    targetWeightKg = if (ex.isNull("targetWeightKg")) null
                                     else ex.optDouble("targetWeightKg", 0.0).toFloat().takeIf { !it.isNaN() },
                    targetWeightsPerSet = ex.optString("targetWeightsPerSet", ""),
                    restSeconds = ex.optInt("restSeconds", 90),
                    notes = ex.optString("notes", "")
                )
            }
            routineRepo.replaceRoutineExercises(newRoutineId, routineExercises)
            true
        } catch (e: Exception) {
            false
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
