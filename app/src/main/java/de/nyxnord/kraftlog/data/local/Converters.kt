package de.nyxnord.kraftlog.data.local

import androidx.room.TypeConverter
import de.nyxnord.kraftlog.data.local.entity.ExerciseCategory
import de.nyxnord.kraftlog.data.local.entity.MuscleGroup

class Converters {

    @TypeConverter
    fun fromExerciseCategory(value: ExerciseCategory): String = value.name

    @TypeConverter
    fun toExerciseCategory(value: String): ExerciseCategory = ExerciseCategory.valueOf(value)

    @TypeConverter
    fun fromMuscleGroupList(value: List<MuscleGroup>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun toMuscleGroupList(value: String): List<MuscleGroup> =
        if (value.isBlank()) emptyList()
        else value.split(",").map { MuscleGroup.valueOf(it) }
}
