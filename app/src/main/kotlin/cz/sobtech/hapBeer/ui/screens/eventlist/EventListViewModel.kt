package cz.sobtech.hapBeer.ui.screens.eventlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.sobtech.hapBeer.data.entity.EventEntity
import cz.sobtech.hapBeer.data.repository.PivoRepository
import cz.sobtech.hapBeer.ui.util.AppLogger
import cz.sobtech.hapBeer.ui.util.LogLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventListViewModel(private val repository: PivoRepository) : ViewModel() {

    val events: StateFlow<List<EventEntity>> = repository.allEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addEvent(name: String, date: Long) {
        viewModelScope.launch {
            repository.insertEvent(EventEntity(name = name, date = date))
            AppLogger.log(LogLevel.INFO, "Akce vytvořena: \"$name\"")
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            AppLogger.log(LogLevel.INFO, "Akce smazána: \"${event.name}\"")
        }
    }

    companion object {
        fun provideFactory(repository: PivoRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { EventListViewModel(repository) }
            }
    }
}
