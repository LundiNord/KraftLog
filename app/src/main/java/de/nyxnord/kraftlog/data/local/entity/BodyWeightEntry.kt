package de.nyxnord.kraftlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_weight_entries")
data class BodyWeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,       // epoch ms (start of day)
    val weightKg: Float
)
