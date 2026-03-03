package de.nyxnord.kraftlog.data.repository

import de.nyxnord.kraftlog.data.local.dao.RoutineDao
import de.nyxnord.kraftlog.data.local.entity.Routine
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise
import de.nyxnord.kraftlog.data.local.relation.RoutineWithExerciseDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoutineRepository(private val dao: RoutineDao) {

    fun getAllRoutines(): Flow<List<Routine>> = dao.getAllRoutines()

    fun getAllRoutinesWithExerciseDetails(): Flow<List<RoutineWithExerciseDetails>> =
        dao.getAllRoutinesWithExerciseDetails().map { list ->
            list.map { r -> r.copy(exerciseDetails = r.exerciseDetails.sortedBy { it.routineExercise.orderIndex }) }
        }

    fun getRoutineWithExerciseDetails(id: Long): Flow<RoutineWithExerciseDetails?> =
        dao.getRoutineWithExerciseDetails(id).map { r ->
            r?.copy(exerciseDetails = r.exerciseDetails.sortedBy { it.routineExercise.orderIndex })
        }

    suspend fun getRoutineById(id: Long): Routine? = dao.getRoutineById(id)

    suspend fun insertRoutine(routine: Routine): Long = dao.insertRoutine(routine)

    suspend fun updateRoutine(routine: Routine) = dao.updateRoutine(routine)

    suspend fun deleteRoutine(routine: Routine) = dao.deleteRoutine(routine)

    suspend fun replaceRoutineExercises(routineId: Long, exercises: List<RoutineExercise>) =
        dao.replaceRoutineExercises(routineId, exercises)
}
