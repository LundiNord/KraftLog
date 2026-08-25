package de.nyxnord.kraftlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: ExerciseCategory,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    @ColumnInfo(defaultValue = "")
    val instructions: String = "",
    @ColumnInfo(defaultValue = "0")
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
