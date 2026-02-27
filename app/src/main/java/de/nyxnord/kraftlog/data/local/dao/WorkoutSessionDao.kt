package de.nyxnord.kraftlog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import de.nyxnord.kraftlog.data.local.relation.WorkoutSessionWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun getActiveSession(): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSessionSync(): WorkoutSession?

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseName, setNumber")
    suspend fun getSetsForSessionList(sessionId: Long): List<WorkoutSet>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun getSessionWithSets(id: Long): Flow<WorkoutSessionWithSets?>

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC LIMIT 1")
    suspend fun getLastFinishedSession(): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    suspend fun getFinishedSessionsList(): List<WorkoutSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("UPDATE workout_sessions SET finishedAt = :finishedAt WHERE id = :id")
    suspend fun finishSession(id: Long, finishedAt: Long)

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM workout_sessions WHERE finishedAt IS NULL")
    suspend fun deleteAllUnfinishedSessions()

    // WorkoutSet operations

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseName, setNumber")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exerciseId ORDER BY loggedAt DESC")
    fun getSetsForExercise(exerciseId: Long): Flow<List<WorkoutSet>>

    @Query("""
        SELECT ws.* FROM workout_sets ws
        WHERE ws.exerciseId = :exerciseId
        AND ws.sessionId = (
            SELECT s.id FROM workout_sessions s
            INNER JOIN workout_sets ws2 ON s.id = ws2.sessionId
            WHERE ws2.exerciseId = :exerciseId
            AND s.id != :excludeSessionId
            ORDER BY s.startedAt DESC
            LIMIT 1
        )
        ORDER BY ws.setNumber ASC
    """)
    suspend fun getLastSessionSetsForExercise(exerciseId: Long, excludeSessionId: Long): List<WorkoutSet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(workoutSet: WorkoutSet): Long

    @Update
    suspend fun updateSet(workoutSet: WorkoutSet)

    @Delete
    suspend fun deleteSet(workoutSet: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND setNumber = :setNumber")
    suspend fun deleteSetByNumber(sessionId: Long, exerciseId: Long, setNumber: Int)

    @Query("SELECT * FROM workout_sets")
    fun getAllSets(): Flow<List<WorkoutSet>>
}
