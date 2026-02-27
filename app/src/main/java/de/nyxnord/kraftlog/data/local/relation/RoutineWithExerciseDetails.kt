package de.nyxnord.kraftlog.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import de.nyxnord.kraftlog.data.local.entity.Routine
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise

data class RoutineWithExerciseDetails(
    @Embedded val routine: Routine,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId",
        entity = RoutineExercise::class
    )
    val exerciseDetails: List<RoutineExerciseWithExercise>
)
