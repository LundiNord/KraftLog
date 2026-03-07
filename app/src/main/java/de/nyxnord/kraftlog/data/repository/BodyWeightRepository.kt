package de.nyxnord.kraftlog.data.repository

import de.nyxnord.kraftlog.data.local.dao.BodyWeightDao
import de.nyxnord.kraftlog.data.local.entity.BodyWeightEntry
import kotlinx.coroutines.flow.Flow

class BodyWeightRepository(private val dao: BodyWeightDao) {
    val entries: Flow<List<BodyWeightEntry>> = dao.getAll()

    suspend fun add(entry: BodyWeightEntry) = dao.insert(entry)

    suspend fun delete(entry: BodyWeightEntry) = dao.delete(entry)
}
