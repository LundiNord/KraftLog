package de.nyxnord.kraftlog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "routine_exercises",
    primaryKeys = ["routineId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index("exerciseId")]
)
data class RoutineExercise(
    val routineId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Float? = null,
    val targetWeightsPerSet: String = "", // comma-separated per-set weights, e.g. "60.0,65.0,70.0"
    val restSeconds: Int = 90,
    val notes: String = ""
)
