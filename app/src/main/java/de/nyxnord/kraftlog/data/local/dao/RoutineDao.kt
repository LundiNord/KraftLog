package de.nyxnord.kraftlog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.nyxnord.kraftlog.data.local.entity.Routine
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise
import de.nyxnord.kraftlog.data.local.relation.RoutineWithExerciseDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines ORDER BY lastUsedAt DESC, createdAt DESC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): Routine?

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun getRoutineWithExerciseDetails(id: Long): Flow<RoutineWithExerciseDetails?>

    @Transaction
    @Query("SELECT * FROM routines ORDER BY lastUsedAt DESC, createdAt DESC")
    fun getAllRoutinesWithExerciseDetails(): Flow<List<RoutineWithExerciseDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(routineExercises: List<RoutineExercise>)

    @Delete
    suspend fun deleteRoutineExercise(routineExercise: RoutineExercise)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteAllExercisesFromRoutine(routineId: Long)

    @Transaction
    suspend fun replaceRoutineExercises(routineId: Long, exercises: List<RoutineExercise>) {
        deleteAllExercisesFromRoutine(routineId)
        insertRoutineExercises(exercises)
    }
}
