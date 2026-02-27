package de.nyxnord.kraftlog.data.repository

import de.nyxnord.kraftlog.data.local.dao.ExerciseDao
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.ExerciseCategory
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val dao: ExerciseDao) {

    fun getAllExercises(): Flow<List<Exercise>> = dao.getAllExercises()

    fun getExercisesByCategory(category: ExerciseCategory): Flow<List<Exercise>> =
        dao.getExercisesByCategory(category)

    fun searchExercises(query: String): Flow<List<Exercise>> = dao.searchExercises(query)

    suspend fun getExerciseById(id: Long): Exercise? = dao.getExerciseById(id)

    suspend fun insertExercise(exercise: Exercise): Long = dao.insertExercise(exercise)

    suspend fun updateExercise(exercise: Exercise) = dao.updateExercise(exercise)

    suspend fun deleteExercise(exercise: Exercise) = dao.deleteExercise(exercise)
}
