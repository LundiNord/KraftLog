package de.nyxnord.kraftlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bouldering_routes",
    foreignKeys = [ForeignKey(
        entity = WorkoutSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class BoulderingRoute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    @ColumnInfo(name = "grade") val description: String,
    val isCompleted: Boolean = true
)
