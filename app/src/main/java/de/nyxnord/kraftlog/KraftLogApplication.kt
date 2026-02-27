package de.nyxnord.kraftlog

import android.app.Application
import de.nyxnord.kraftlog.data.local.KraftLogDatabase
import de.nyxnord.kraftlog.data.repository.ExerciseRepository
import de.nyxnord.kraftlog.data.repository.RoutineRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository

class KraftLogApplication : Application() {

    val database by lazy { KraftLogDatabase.getInstance(this) }

    val exerciseRepository by lazy { ExerciseRepository(database.exerciseDao()) }
    val routineRepository by lazy { RoutineRepository(database.routineDao()) }
    val workoutRepository by lazy { WorkoutRepository(database.workoutSessionDao()) }
}
