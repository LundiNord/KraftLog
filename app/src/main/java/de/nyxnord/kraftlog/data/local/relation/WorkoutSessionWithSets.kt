package de.nyxnord.kraftlog.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet

data class WorkoutSessionWithSets(
    @Embedded val session: WorkoutSession,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val sets: List<WorkoutSet>
)
