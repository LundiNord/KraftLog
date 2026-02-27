package de.nyxnord.kraftlog

import android.app.Application
import androidx.glance.appwidget.updateAll
import de.nyxnord.kraftlog.data.local.KraftLogDatabase
import de.nyxnord.kraftlog.data.preferences.ReminderPreferences
import de.nyxnord.kraftlog.data.repository.AlternativeWorkoutRepository
import de.nyxnord.kraftlog.data.repository.ExerciseRepository
import de.nyxnord.kraftlog.data.repository.RoutineRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import de.nyxnord.kraftlog.notification.ReminderScheduler
import de.nyxnord.kraftlog.widget.KraftLogWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KraftLogApplication : Application() {

    val database by lazy { KraftLogDatabase.getInstance(this) }

    val exerciseRepository by lazy { ExerciseRepository(database.exerciseDao()) }
    val routineRepository by lazy { RoutineRepository(database.routineDao()) }
    val workoutRepository by lazy { WorkoutRepository(database.workoutSessionDao()) }
    val alternativeWorkoutRepository by lazy { AlternativeWorkoutRepository(database.alternativeWorkoutDao()) }
    val reminderPreferences by lazy { ReminderPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        if (reminderPreferences.enabled) {
            ReminderScheduler.schedule(this)
        }
    }

    fun updateWidget() {
        CoroutineScope(Dispatchers.IO).launch {
            KraftLogWidget().updateAll(this@KraftLogApplication)
        }
    }
}
