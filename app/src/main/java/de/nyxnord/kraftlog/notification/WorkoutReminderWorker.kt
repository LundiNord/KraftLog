package de.nyxnord.kraftlog.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.nyxnord.kraftlog.MainActivity
import de.nyxnord.kraftlog.R
import de.nyxnord.kraftlog.data.local.KraftLogDatabase
import de.nyxnord.kraftlog.data.preferences.ReminderPreferences
import java.util.concurrent.TimeUnit

class WorkoutReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = ReminderPreferences(context)
        if (!prefs.enabled) return Result.success()

        val db = KraftLogDatabase.getInstance(context)
        val last = db.workoutSessionDao().getLastFinishedSession()
        val daysSince = if (last == null) Long.MAX_VALUE
                        else TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - last.finishedAt!!)

        if (daysSince >= prefs.intervalDays) {
            postNotification(daysSince)
        }
        return Result.success()
    }

    private fun postNotification(daysSince: Long) {
        val channelId = "workout_reminder"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId, "Workout Reminders", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Reminds you to log a workout" }
        nm.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (daysSince == Long.MAX_VALUE)
            "No workouts logged yet. Start your first session!"
        else
            "You haven't worked out in $daysSince day${if (daysSince == 1L) "" else "s"}. Time to hit the gym!"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to work out!")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
