package de.nyxnord.kraftlog.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.nyxnord.kraftlog.data.local.entity.BodyWeightEntry
import de.nyxnord.kraftlog.data.repository.BodyWeightRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class WeightViewModel(private val repo: BodyWeightRepository) : ViewModel() {

    val entries: StateFlow<List<BodyWeightEntry>> = repo.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addEntry(weightKg: Float, dateMs: Long) {
        viewModelScope.launch {
            repo.add(BodyWeightEntry(date = dateMs, weightKg = weightKg))
        }
    }

    fun deleteEntry(entry: BodyWeightEntry) {
        viewModelScope.launch { repo.delete(entry) }
    }

    companion object {
        fun factory(repo: BodyWeightRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WeightViewModel(repo) as T
        }
    }
}

fun todayStartMs(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
