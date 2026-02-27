package de.nyxnord.kraftlog.data.repository

import de.nyxnord.kraftlog.data.local.dao.AlternativeWorkoutDao
import de.nyxnord.kraftlog.data.local.entity.BoulderingRoute
import de.nyxnord.kraftlog.data.local.entity.RunningEntry
import kotlinx.coroutines.flow.Flow

class AlternativeWorkoutRepository(private val dao: AlternativeWorkoutDao) {

    suspend fun upsertRunningEntry(entry: RunningEntry) = dao.upsertRunningEntry(entry)
    fun getRunningEntry(sessionId: Long): Flow<RunningEntry?> = dao.getRunningEntry(sessionId)
    suspend fun getRunningEntrySync(sessionId: Long): RunningEntry? = dao.getRunningEntrySync(sessionId)

    suspend fun insertBoulderingRoute(route: BoulderingRoute): Long = dao.insertBoulderingRoute(route)
    suspend fun deleteBoulderingRoute(route: BoulderingRoute) = dao.deleteBoulderingRoute(route)
    fun getBoulderingRoutes(sessionId: Long): Flow<List<BoulderingRoute>> = dao.getBoulderingRoutes(sessionId)
    suspend fun getBoulderingRoutesSync(sessionId: Long): List<BoulderingRoute> = dao.getBoulderingRoutesSync(sessionId)
}
