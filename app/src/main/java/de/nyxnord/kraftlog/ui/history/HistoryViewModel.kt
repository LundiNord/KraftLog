package de.nyxnord.kraftlog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.WorkoutSession
import de.nyxnord.kraftlog.data.local.relation.WorkoutSessionWithSets
import de.nyxnord.kraftlog.data.repository.WorkoutRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(private val workoutRepo: WorkoutRepository) : ViewModel() {

    val sessions: StateFlow<List<WorkoutSession>> =
        workoutRepo.getAllSessions()
            .map { list -> list.filter { it.finishedAt != null } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getSessionDetail(id: Long): StateFlow<WorkoutSessionWithSets?> =
        workoutRepo.getSessionWithSets(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch { workoutRepo.deleteSession(session) }
    }

    companion object {
        fun factory(workoutRepo: WorkoutRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(workoutRepo) as T
            }
    }
}
