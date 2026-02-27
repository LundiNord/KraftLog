package de.nyxnord.kraftlog.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.nyxnord.kraftlog.MainActivity
import de.nyxnord.kraftlog.data.local.KraftLogDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val EXTRA_START_ROUTINE_ID = "start_routine_id"

class KraftLogWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = KraftLogDatabase.getInstance(context).workoutSessionDao()
        val sessions = dao.getFinishedSessionsList()

        fun periodStart(field: Int): Long {
            val cal = Calendar.getInstance()
            when (field) {
                Calendar.DAY_OF_WEEK -> cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                Calendar.DAY_OF_MONTH -> cal.set(Calendar.DAY_OF_MONTH, 1)
            }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val weekSessions = sessions.count { it.startedAt >= periodStart(Calendar.DAY_OF_WEEK) }
        val monthSessions = sessions.count { it.startedAt >= periodStart(Calendar.DAY_OF_MONTH) }
        val lastSession = sessions.firstOrNull()
        val lastDaysAgo = lastSession?.finishedAt?.let {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it)
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    context = context,
                    weekSessions = weekSessions,
                    monthSessions = monthSessions,
                    lastSessionName = lastSession?.name,
                    lastSessionDaysAgo = lastDaysAgo
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    context: Context,
    weekSessions: Int,
    monthSessions: Int,
    lastSessionName: String?,
    lastSessionDaysAgo: Long?
) {
    val startIntent = Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_START_ROUTINE_ID, -1L)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(14.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            // Header
            Text(
                text = "KraftLog",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = GlanceTheme.colors.primary
                )
            )

            Spacer(GlanceModifier.height(10.dp))

            // Session count stats
            Row(modifier = GlanceModifier.fillMaxWidth().wrapContentHeight()) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "$weekSessions",
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Text(
                        text = "this week",
                        style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.secondary)
                    )
                }
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "$monthSessions",
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Text(
                        text = "this month",
                        style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.secondary)
                    )
                }
            }

            Spacer(GlanceModifier.height(8.dp))

            // Last workout line
            val lastText = when {
                lastSessionName == null -> "No workouts yet"
                lastSessionDaysAgo == null || lastSessionDaysAgo == 0L ->
                    "Last: $lastSessionName · today"
                lastSessionDaysAgo == 1L -> "Last: $lastSessionName · yesterday"
                else -> "Last: $lastSessionName · ${lastSessionDaysAgo}d ago"
            }
            Text(
                text = lastText,
                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.secondary),
                maxLines = 1
            )

            Spacer(GlanceModifier.height(10.dp))

            // Quick workout button
            androidx.glance.Button(
                text = "Quick Workout",
                onClick = actionStartActivity(startIntent),
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }
}
