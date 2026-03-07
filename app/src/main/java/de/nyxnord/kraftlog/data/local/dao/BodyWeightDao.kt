package de.nyxnord.kraftlog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.nyxnord.kraftlog.data.local.entity.BodyWeightEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weight_entries ORDER BY date DESC")
    fun getAll(): Flow<List<BodyWeightEntry>>

    @Insert
    suspend fun insert(entry: BodyWeightEntry)

    @Delete
    suspend fun delete(entry: BodyWeightEntry)
}
