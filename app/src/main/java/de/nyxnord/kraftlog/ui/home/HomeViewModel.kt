package de.nyxnord.kraftlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.repository.RoutineRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import de.nyxnord.kraftlog.data.local.relation.RoutineWithExerciseDetails
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class WeeklyStats(val sessions: Int, val totalVolumeKg: Float)

data class HomeUiState(
    val routines: List<RoutineWithExerciseDetails> = emptyList(),
    val recentSessions: List<WorkoutSession> = emptyList(),
    val weeklyStats: WeeklyStats = WeeklyStats(0, 0f),
    val activeSession: WorkoutSession? = null
)

class HomeViewModel(
    routineRepo: RoutineRepository,
    workoutRepo: WorkoutRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        routineRepo.getAllRoutinesWithExerciseDetails(),
        workoutRepo.getRecentSessions(5),
        workoutRepo.getActiveSession()
    ) { routines, recent, active ->
        val weekStart = weekStartMillis()
        val weekSessions = recent.filter { it.startedAt >= weekStart }
        HomeUiState(
            routines = routines,
            recentSessions = recent,
            weeklyStats = WeeklyStats(weekSessions.size, 0f),
            activeSession = active
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun weekStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        fun factory(routineRepo: RoutineRepository, workoutRepo: WorkoutRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(routineRepo, workoutRepo) as T
            }
    }
}
