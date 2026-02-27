package de.nyxnord.kraftlog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import de.nyxnord.kraftlog.data.local.dao.ExerciseDao
import de.nyxnord.kraftlog.data.local.dao.RoutineDao
import de.nyxnord.kraftlog.data.local.dao.WorkoutSessionDao
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.ExerciseCategory
import de.nyxnord.kraftlog.data.local.entity.MuscleGroup
import de.nyxnord.kraftlog.data.local.entity.Routine
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Exercise::class, Routine::class, RoutineExercise::class, WorkoutSession::class, WorkoutSet::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KraftLogDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        @Volatile
        private var INSTANCE: KraftLogDatabase? = null

        fun getInstance(context: Context): KraftLogDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    KraftLogDatabase::class.java,
                    "kraftlog.db"
                )
                    .addCallback(SeedCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedExercises(database.exerciseDao())
                }
            }
        }

        private suspend fun seedExercises(dao: ExerciseDao) {
            val exercises = listOf(
                // Chest
                Exercise(name = "Bench Press", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CHEST),
                    secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                    instructions = "Lie on a flat bench. Lower the bar to your chest, then press back up."),
                Exercise(name = "Incline Bench Press", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CHEST),
                    secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)),
                Exercise(name = "Push-Up", category = ExerciseCategory.CALISTHENICS,
                    primaryMuscles = listOf(MuscleGroup.CHEST),
                    secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.CORE)),
                Exercise(name = "Dumbbell Fly", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CHEST),
                    secondaryMuscles = listOf(MuscleGroup.SHOULDERS)),
                // Back
                Exercise(name = "Deadlift", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BACK),
                    secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
                    instructions = "Stand with feet hip-width. Hinge at hips, grip bar, drive hips forward to stand."),
                Exercise(name = "Pull-Up", category = ExerciseCategory.CALISTHENICS,
                    primaryMuscles = listOf(MuscleGroup.BACK),
                    secondaryMuscles = listOf(MuscleGroup.BICEPS)),
                Exercise(name = "Barbell Row", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BACK),
                    secondaryMuscles = listOf(MuscleGroup.BICEPS)),
                Exercise(name = "Lat Pulldown", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BACK),
                    secondaryMuscles = listOf(MuscleGroup.BICEPS)),
                Exercise(name = "Seated Cable Row", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BACK),
                    secondaryMuscles = listOf(MuscleGroup.BICEPS)),
                // Shoulders
                Exercise(name = "Overhead Press", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.SHOULDERS),
                    secondaryMuscles = listOf(MuscleGroup.TRICEPS),
                    instructions = "Press barbell from shoulder height to overhead lockout."),
                Exercise(name = "Lateral Raise", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.SHOULDERS)),
                Exercise(name = "Face Pull", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.SHOULDERS),
                    secondaryMuscles = listOf(MuscleGroup.BACK)),
                // Arms
                Exercise(name = "Barbell Curl", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BICEPS),
                    secondaryMuscles = listOf(MuscleGroup.FOREARMS)),
                Exercise(name = "Dumbbell Curl", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BICEPS)),
                Exercise(name = "Tricep Pushdown", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.TRICEPS)),
                Exercise(name = "Skull Crusher", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.TRICEPS)),
                Exercise(name = "Dip", category = ExerciseCategory.CALISTHENICS,
                    primaryMuscles = listOf(MuscleGroup.TRICEPS),
                    secondaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS)),
                // Legs
                Exercise(name = "Squat", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.QUADRICEPS),
                    secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
                    instructions = "Bar on upper back. Descend until thighs parallel. Drive up through heels."),
                Exercise(name = "Romanian Deadlift", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.HAMSTRINGS),
                    secondaryMuscles = listOf(MuscleGroup.GLUTES)),
                Exercise(name = "Leg Press", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.QUADRICEPS),
                    secondaryMuscles = listOf(MuscleGroup.GLUTES)),
                Exercise(name = "Leg Curl", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.HAMSTRINGS)),
                Exercise(name = "Leg Extension", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.QUADRICEPS)),
                Exercise(name = "Calf Raise", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CALVES)),
                Exercise(name = "Lunge", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.QUADRICEPS),
                    secondaryMuscles = listOf(MuscleGroup.GLUTES)),
                Exercise(name = "Hip Thrust", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.GLUTES),
                    secondaryMuscles = listOf(MuscleGroup.HAMSTRINGS)),
                // Core
                Exercise(name = "Plank", category = ExerciseCategory.CALISTHENICS,
                    primaryMuscles = listOf(MuscleGroup.CORE)),
                Exercise(name = "Crunch", category = ExerciseCategory.CALISTHENICS,
                    primaryMuscles = listOf(MuscleGroup.CORE)),
                Exercise(name = "Hanging Leg Raise", category = ExerciseCategory.CALISTHENICS,
                    primaryMuscles = listOf(MuscleGroup.CORE)),
                // Cardio
                Exercise(name = "Running", category = ExerciseCategory.CARDIO,
                    primaryMuscles = listOf(MuscleGroup.FULL_BODY)),
                Exercise(name = "Cycling", category = ExerciseCategory.CARDIO,
                    primaryMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.CALVES)),
                Exercise(name = "Jump Rope", category = ExerciseCategory.CARDIO,
                    primaryMuscles = listOf(MuscleGroup.FULL_BODY)),
            )
            dao.insertExercises(exercises)
        }
    }
}
