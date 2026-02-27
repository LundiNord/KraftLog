package de.nyxnord.kraftlog.data.repository

import de.nyxnord.kraftlog.data.local.dao.WorkoutSessionDao
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import de.nyxnord.kraftlog.data.local.relation.WorkoutSessionWithSets
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val dao: WorkoutSessionDao) {

    fun getAllSessions(): Flow<List<WorkoutSession>> = dao.getAllSessions()

    fun getActiveSession(): Flow<WorkoutSession?> = dao.getActiveSession()

    fun getSessionWithSets(id: Long): Flow<WorkoutSessionWithSets?> = dao.getSessionWithSets(id)

    fun getRecentSessions(limit: Int = 10): Flow<List<WorkoutSession>> = dao.getRecentSessions(limit)

    fun getAllSets(): Flow<List<WorkoutSet>> = dao.getAllSets()

    suspend fun getLastFinishedSession(): WorkoutSession? = dao.getLastFinishedSession()

    suspend fun getFinishedSessionsList(): List<WorkoutSession> = dao.getFinishedSessionsList()

    fun getSetsForExercise(exerciseId: Long): Flow<List<WorkoutSet>> = dao.getSetsForExercise(exerciseId)

    suspend fun insertSession(session: WorkoutSession): Long = dao.insertSession(session)

    suspend fun updateSession(session: WorkoutSession) = dao.updateSession(session)

    suspend fun finishSession(id: Long) = dao.finishSession(id, System.currentTimeMillis())

    suspend fun deleteSession(session: WorkoutSession) = dao.deleteSession(session)

    suspend fun deleteSessionById(id: Long) = dao.deleteSessionById(id)

    suspend fun deleteAllUnfinishedSessions() = dao.deleteAllUnfinishedSessions()

    suspend fun getLastSessionSetsForExercise(exerciseId: Long, excludeSessionId: Long): List<WorkoutSet> =
        dao.getLastSessionSetsForExercise(exerciseId, excludeSessionId)

    suspend fun insertSet(workoutSet: WorkoutSet): Long = dao.insertSet(workoutSet)

    suspend fun updateSet(workoutSet: WorkoutSet) = dao.updateSet(workoutSet)

    suspend fun deleteSet(workoutSet: WorkoutSet) = dao.deleteSet(workoutSet)
}
