package de.nyxnord.kraftlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.SessionType
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.repository.RoutineRepository
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import de.nyxnord.kraftlog.data.local.relation.RoutineWithExerciseDetails
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class HomeUiState(
    val routines: List<RoutineWithExerciseDetails> = emptyList(),
    val recentSessions: List<WorkoutSession> = emptyList(),
    val weeklySessions: Int = 0,
    val monthlySessions: Int = 0,
    val yearlySessions: Int = 0,
    val activeSession: WorkoutSession? = null
)

class HomeViewModel(
    routineRepo: RoutineRepository,
    workoutRepo: WorkoutRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        routineRepo.getAllRoutinesWithExerciseDetails(),
        workoutRepo.getAllSessions(),
        workoutRepo.getActiveSession()
    ) { routines, allSessions, active ->
        val finished = allSessions.filter { it.finishedAt != null }
        val weekStart = periodStartMillis(Calendar.DAY_OF_WEEK)
        val monthStart = periodStartMillis(Calendar.DAY_OF_MONTH)
        val yearStart = periodStartMillis(Calendar.DAY_OF_YEAR)
        HomeUiState(
            routines = routines,
            recentSessions = finished.take(5),
            weeklySessions = finished.count { it.startedAt >= weekStart },
            monthlySessions = finished.count { it.startedAt >= monthStart },
            yearlySessions = finished.count { it.startedAt >= yearStart },
            activeSession = if (active?.sessionType == SessionType.STRENGTH.name) active else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun periodStartMillis(calendarField: Int): Long {
        val cal = Calendar.getInstance()
        when (calendarField) {
            Calendar.DAY_OF_WEEK -> cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            Calendar.DAY_OF_MONTH -> cal.set(Calendar.DAY_OF_MONTH, 1)
            Calendar.DAY_OF_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
            }
        }
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
