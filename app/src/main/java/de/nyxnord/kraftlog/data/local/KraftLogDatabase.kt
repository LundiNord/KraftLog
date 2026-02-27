package de.nyxnord.kraftlog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.nyxnord.kraftlog.data.local.dao.AlternativeWorkoutDao
import de.nyxnord.kraftlog.data.local.dao.ExerciseDao
import de.nyxnord.kraftlog.data.local.dao.RoutineDao
import de.nyxnord.kraftlog.data.local.dao.WorkoutSessionDao
import de.nyxnord.kraftlog.data.local.entity.BoulderingRoute
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.ExerciseCategory
import de.nyxnord.kraftlog.data.local.entity.MuscleGroup
import de.nyxnord.kraftlog.data.local.entity.Routine
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise
import de.nyxnord.kraftlog.data.local.entity.RunningEntry
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Exercise::class, Routine::class, RoutineExercise::class, WorkoutSession::class, WorkoutSet::class,
                RunningEntry::class, BoulderingRoute::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KraftLogDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun alternativeWorkoutDao(): AlternativeWorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: KraftLogDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE routine_exercises ADD COLUMN targetWeightsPerSet TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE routine_exercises ADD COLUMN targetRepsPerSet TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE workout_sessions ADD COLUMN sessionType TEXT NOT NULL DEFAULT 'STRENGTH'"
                )
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS running_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        distanceKm REAL NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_running_entries_sessionId ON running_entries(sessionId)")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS bouldering_routes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        grade TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_bouldering_routes_sessionId ON bouldering_routes(sessionId)")
            }
        }

        fun getInstance(context: Context): KraftLogDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    KraftLogDatabase::class.java,
                    "kraftlog.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(SeedCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedExercises(database.exerciseDao())
                    seedRoutines(database.exerciseDao(), database.routineDao())
                }
            }
        }

        private suspend fun seedExercises(dao: ExerciseDao) {
            val exercises = listOf(
                // Workout 31.07.2025 (Chest, Triceps, Shoulders)
                Exercise(name = "Brustpresse (01)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CHEST),
                    secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)),
                Exercise(name = "Schulterpresse (06)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.SHOULDERS)),
                Exercise(name = "Trizepsmaschine (23)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.TRICEPS)),
                Exercise(name = "Bankdrücken schräg (38)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CHEST),
                    secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)),
                Exercise(name = "Plate Loaded Seated Dip", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.TRICEPS),
                    secondaryMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS)),
                Exercise(name = "Bankdrücken (25)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CHEST),
                    secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)),
                Exercise(name = "Trizepsstrecken beidarmig sitzend", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.TRICEPS)),
                Exercise(name = "Seitheben (21)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.SHOULDERS)),
                Exercise(name = "Butterfly (02)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CHEST)),

                // Workout 05.08.2025 (Legs, Core)
                Exercise(name = "Beinstreckung (14)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.QUADRICEPS)),
                Exercise(name = "Beinbeuger liegend", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.HAMSTRINGS)),
                Exercise(name = "Adduktion (09)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.QUADRICEPS)),
                Exercise(name = "Abduktion (08)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.GLUTES)),
                Exercise(name = "Beinpresse (07)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.QUADRICEPS),
                    secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.CALVES)),
                Exercise(name = "Bauchmaschine (HS)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CORE)),
                Exercise(name = "Wadenheben stehend", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CALVES)),
                Exercise(name = "Rumpfrotation (120)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.CORE)),

                // Workout 02.08.2025 (Back, Biceps)
                Exercise(name = "Upper Back (03A)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BACK)),
                Exercise(name = "Vertical Traction (05A)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BACK),
                    secondaryMuscles = listOf(MuscleGroup.BICEPS)),
                Exercise(name = "Bizepsmaschine (22)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BICEPS)),
                Exercise(name = "Reverse Fly", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BACK),
                    secondaryMuscles = listOf(MuscleGroup.SHOULDERS)),
                Exercise(name = "Rückenstreckung 45°", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BACK),
                    secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS)),
                Exercise(name = "PL Latzug (50)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BACK),
                    secondaryMuscles = listOf(MuscleGroup.BICEPS)),
                Exercise(name = "Rudern sitzend", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BACK),
                    secondaryMuscles = listOf(MuscleGroup.BICEPS)),
                Exercise(name = "Bizeps Curls stehend (95)", category = ExerciseCategory.STRENGTH,
                    primaryMuscles = listOf(MuscleGroup.BICEPS))
            )
            exercises.forEach { exercise ->
                if (dao.getByName(exercise.name) == null) {
                    dao.insertExercise(exercise)
                }
            }
        }

        private suspend fun seedRoutines(exerciseDao: ExerciseDao, routineDao: RoutineDao) {
            suspend fun insertRoutine(name: String, exerciseNames: List<String>) {
                if (routineDao.getByName(name) != null) return
                val routineId = routineDao.insertRoutine(Routine(name = name))
                val routineExercises = exerciseNames.mapIndexedNotNull { idx, exName ->
                    exerciseDao.getByName(exName)?.let { ex ->
                        RoutineExercise(routineId = routineId, exerciseId = ex.id, orderIndex = idx)
                    }
                }
                routineDao.replaceRoutineExercises(routineId, routineExercises)
            }

            insertRoutine(
                "Brust, Trizeps & Schultern",
                listOf(
                    "Brustpresse (01)", "Bankdrücken schräg (38)", "Bankdrücken (25)",
                    "Butterfly (02)", "Schulterpresse (06)", "Seitheben (21)",
                    "Trizepsmaschine (23)", "Plate Loaded Seated Dip", "Trizepsstrecken beidarmig sitzend"
                )
            )
            insertRoutine(
                "Beine & Core",
                listOf(
                    "Beinpresse (07)", "Beinstreckung (14)", "Beinbeuger liegend",
                    "Adduktion (09)", "Abduktion (08)", "Wadenheben stehend",
                    "Bauchmaschine (HS)", "Rumpfrotation (120)"
                )
            )
            insertRoutine(
                "Rücken & Bizeps",
                listOf(
                    "Upper Back (03A)", "PL Latzug (50)", "Vertical Traction (05A)",
                    "Rudern sitzend", "Reverse Fly", "Rückenstreckung 45°",
                    "Bizepsmaschine (22)", "Bizeps Curls stehend (95)"
                )
            )
        }
    }
}