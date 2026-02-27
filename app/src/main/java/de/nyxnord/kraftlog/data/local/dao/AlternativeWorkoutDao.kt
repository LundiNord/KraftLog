package de.nyxnord.kraftlog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.nyxnord.kraftlog.data.local.entity.BoulderingRoute
import de.nyxnord.kraftlog.data.local.entity.RunningEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AlternativeWorkoutDao {

    // Running
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRunningEntry(entry: RunningEntry): Long

    @Query("SELECT * FROM running_entries WHERE sessionId = :sessionId LIMIT 1")
    fun getRunningEntry(sessionId: Long): Flow<RunningEntry?>

    @Query("SELECT * FROM running_entries WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getRunningEntrySync(sessionId: Long): RunningEntry?

    // Bouldering
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoulderingRoute(route: BoulderingRoute): Long

    @Delete
    suspend fun deleteBoulderingRoute(route: BoulderingRoute)

    @Query("SELECT * FROM bouldering_routes WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getBoulderingRoutes(sessionId: Long): Flow<List<BoulderingRoute>>

    @Query("SELECT * FROM bouldering_routes WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getBoulderingRoutesSync(sessionId: Long): List<BoulderingRoute>
}
