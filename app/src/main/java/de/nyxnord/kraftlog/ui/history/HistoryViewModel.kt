package de.nyxnord.kraftlog.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.BoulderingRoute
import de.nyxnord.kraftlog.data.local.entity.Exercise
import de.nyxnord.kraftlog.data.local.entity.ExerciseCategory
import de.nyxnord.kraftlog.data.local.entity.Routine
import de.nyxnord.kraftlog.data.local.entity.RoutineExercise
import de.nyxnord.kraftlog.data.local.entity.RunningEntry
import de.nyxnord.kraftlog.data.local.entity.SessionType
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.entity.WorkoutSet
import de.nyxnord.kraftlog.data.local.relation.WorkoutSessionWithSets
import de.nyxnord.kraftlog.data.preferences.ReminderPreferences
import de.nyxnord.kraftlog.data.repository.AlternativeWorkoutRepository
import de.nyxnord.kraftlog.data.repository.ExerciseRepository
import de.nyxnord.kraftlog.data.repository.RoutineRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import de.nyxnord.kraftlog.notification.ReminderScheduler
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONArray
import org.json.JSONObject

data class LifetimeStats(val sessions: Int, val totalVolumeKg: Double, val totalReps: Int)

class HistoryViewModel(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
    private val routineRepo: RoutineRepository,
    private val reminderPreferences: ReminderPreferences,
    private val altRepo: AlternativeWorkoutRepository
) : ViewModel() {

    private val _reminderEnabled = MutableStateFlow(reminderPreferences.enabled)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderIntervalDays = MutableStateFlow(reminderPreferences.intervalDays)
    val reminderIntervalDays: StateFlow<Int> = _reminderIntervalDays.asStateFlow()

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        reminderPreferences.enabled = enabled
        _reminderEnabled.value = enabled
        if (enabled) ReminderScheduler.schedule(context)
        else ReminderScheduler.cancel(context)
    }

    fun setReminderIntervalDays(context: Context, days: Int) {
        reminderPreferences.intervalDays = days
        _reminderIntervalDays.value = days
        if (reminderPreferences.enabled) ReminderScheduler.reschedule(context)
    }

    val sessions: StateFlow<List<WorkoutSession>> =
        workoutRepo.getAllSessions()
            .map { list -> list.filter { it.finishedAt != null } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allExercises: StateFlow<List<Exercise>> =
        exerciseRepo.getAllExercises()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lifetimeStats: StateFlow<LifetimeStats> = combine(
        workoutRepo.getAllSessions().map { list -> list.filter { it.finishedAt != null } },
        workoutRepo.getAllSets()
    ) { finishedSessions, sets ->
        LifetimeStats(
            sessions = finishedSessions.size,
            totalVolumeKg = sets.sumOf { (it.weightKg * it.reps).toDouble() },
            totalReps = sets.sumOf { it.reps }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LifetimeStats(0, 0.0, 0))

    // Cached per session ID so recompositions don't recreate flows starting from null
    private val sessionDetailCache = HashMap<Long, StateFlow<WorkoutSessionWithSets?>>()
    fun getSessionDetail(id: Long): StateFlow<WorkoutSessionWithSets?> =
        sessionDetailCache.getOrPut(id) {
            workoutRepo.getSessionWithSets(id)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    private val runningEntryCache = HashMap<Long, StateFlow<RunningEntry?>>()
    fun getRunningEntry(sessionId: Long): StateFlow<RunningEntry?> =
        runningEntryCache.getOrPut(sessionId) {
            altRepo.getRunningEntry(sessionId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    private val boulderingRoutesCache = HashMap<Long, StateFlow<List<BoulderingRoute>>>()
    fun getBoulderingRoutes(sessionId: Long): StateFlow<List<BoulderingRoute>> =
        boulderingRoutesCache.getOrPut(sessionId) {
            altRepo.getBoulderingRoutes(sessionId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch { workoutRepo.deleteSession(session) }
    }

    fun createRoutineFromSession(name: String, sets: List<WorkoutSet>, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val routineId = routineRepo.insertRoutine(Routine(name = name))
            val uniqueExerciseIds = sets.map { it.exerciseId }.distinct()
            val routineExercises = uniqueExerciseIds.mapIndexed { idx, exerciseId ->
                val exerciseSets = sets.filter { it.exerciseId == exerciseId }
                RoutineExercise(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    orderIndex = idx,
                    targetSets = exerciseSets.size,
                    targetReps = exerciseSets.firstOrNull()?.reps ?: 10,
                    targetWeightKg = if (exerciseSets.all { it.isBodyweight }) null
                                     else exerciseSets.filter { !it.isBodyweight }.maxOfOrNull { it.weightKg }
                )
            }
            routineRepo.replaceRoutineExercises(routineId, routineExercises)
            onCreated(routineId)
        }
    }

    suspend fun exportHistoryJson(): String {
        val sessions = workoutRepo.getFinishedSessionsList()
        val sessionsArray = JSONArray()
        for (session in sessions) {
            val sessionObj = JSONObject().apply {
                put("name", session.name)
                put("startedAt", session.startedAt)
                put("finishedAt", session.finishedAt)
                put("notes", session.notes)
                put("sessionType", session.sessionType)
            }
            when (session.sessionType) {
                SessionType.STRENGTH.name -> {
                    val sets = workoutRepo.getSetsForSessionList(session.id)
                    val setsArray = JSONArray()
                    sets.forEach { set ->
                        setsArray.put(JSONObject().apply {
                            put("exerciseName", set.exerciseName)
                            put("exerciseId", set.exerciseId)
                            put("setNumber", set.setNumber)
                            put("reps", set.reps)
                            put("weightKg", set.weightKg.toDouble())
                            put("isBodyweight", set.isBodyweight)
                            if (set.rpe != null) put("rpe", set.rpe.toDouble()) else put("rpe", JSONObject.NULL)
                            put("loggedAt", set.loggedAt)
                        })
                    }
                    sessionObj.put("sets", setsArray)
                }
                SessionType.RUNNING.name -> {
                    val entry = altRepo.getRunningEntrySync(session.id)
                    if (entry != null) {
                        sessionObj.put("runningEntry", JSONObject().apply {
                            put("distanceKm", entry.distanceKm.toDouble())
                            put("durationSeconds", entry.durationSeconds)
                        })
                    }
                }
                SessionType.BOULDERING.name -> {
                    val routes = altRepo.getBoulderingRoutesSync(session.id)
                    val routesArray = JSONArray()
                    routes.forEach { route ->
                        routesArray.put(JSONObject().apply {
                            put("description", route.description)
                            put("isCompleted", route.isCompleted)
                        })
                    }
                    sessionObj.put("boulderingRoutes", routesArray)
                }
            }
            sessionsArray.put(sessionObj)
        }
        return JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("sessions", sessionsArray)
        }.toString(2)
    }

    suspend fun importHistoryFromJson(json: String): Boolean {
        return try {
            val obj = JSONObject(json)
            val sessionsArray = obj.getJSONArray("sessions")
            val existingTimestamps = workoutRepo.getFinishedSessionsList().map { it.startedAt }.toSet()
            for (i in 0 until sessionsArray.length()) {
                val sessionObj = sessionsArray.getJSONObject(i)
                val startedAt = sessionObj.getLong("startedAt")
                if (startedAt in existingTimestamps) continue
                val sessionType = sessionObj.optString("sessionType", SessionType.STRENGTH.name)
                val newSessionId = workoutRepo.insertSession(
                    WorkoutSession(
                        name = sessionObj.getString("name"),
                        startedAt = startedAt,
                        finishedAt = sessionObj.optLong("finishedAt", startedAt),
                        notes = sessionObj.optString("notes", ""),
                        sessionType = sessionType
                    )
                )
                when (sessionType) {
                    SessionType.STRENGTH.name -> {
                        val setsArray = sessionObj.optJSONArray("sets") ?: JSONArray()
                        for (j in 0 until setsArray.length()) {
                            val setObj = setsArray.getJSONObject(j)
                            val exerciseName = setObj.getString("exerciseName")
                            val exerciseId = setObj.optLong("exerciseId", 0L)
                            val exercise = exerciseRepo.getExerciseById(exerciseId)
                                ?: exerciseRepo.getByName(exerciseName)
                                ?: run {
                                    val newId = exerciseRepo.insertExercise(
                                        Exercise(
                                            name = exerciseName,
                                            category = ExerciseCategory.STRENGTH,
                                            primaryMuscles = emptyList(),
                                            isCustom = true
                                        )
                                    )
                                    exerciseRepo.getExerciseById(newId)!!
                                }
                            workoutRepo.insertSet(
                                WorkoutSet(
                                    sessionId = newSessionId,
                                    exerciseId = exercise.id,
                                    exerciseName = exercise.name,
                                    setNumber = setObj.optInt("setNumber", j + 1),
                                    reps = setObj.optInt("reps", 0),
                                    weightKg = setObj.optDouble("weightKg", 0.0).toFloat(),
                                    isBodyweight = setObj.optBoolean("isBodyweight", false),
                                    rpe = if (setObj.isNull("rpe")) null else setObj.optDouble("rpe").toFloat(),
                                    loggedAt = setObj.optLong("loggedAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                    SessionType.RUNNING.name -> {
                        val entryObj = sessionObj.optJSONObject("runningEntry")
                        if (entryObj != null) {
                            altRepo.upsertRunningEntry(
                                RunningEntry(
                                    sessionId = newSessionId,
                                    distanceKm = entryObj.optDouble("distanceKm", 0.0).toFloat(),
                                    durationSeconds = entryObj.optLong("durationSeconds", 0)
                                )
                            )
                        }
                    }
                    SessionType.BOULDERING.name -> {
                        val routesArray = sessionObj.optJSONArray("boulderingRoutes") ?: JSONArray()
                        for (j in 0 until routesArray.length()) {
                            val routeObj = routesArray.getJSONObject(j)
                            altRepo.insertBoulderingRoute(
                                BoulderingRoute(
                                    sessionId = newSessionId,
                                    description = routeObj.getString("description"),
                                    isCompleted = routeObj.optBoolean("isCompleted", true)
                                )
                            )
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun factory(
            workoutRepo: WorkoutRepository,
            exerciseRepo: ExerciseRepository,
            routineRepo: RoutineRepository,
            reminderPreferences: ReminderPreferences,
            altRepo: AlternativeWorkoutRepository
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HistoryViewModel(workoutRepo, exerciseRepo, routineRepo, reminderPreferences, altRepo) as T
        }
    }
}
