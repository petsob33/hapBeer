package cz.sobtech.hapBeer.ui.screens.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.sobtech.hapBeer.data.entity.BeerRecordEntity
import cz.sobtech.hapBeer.data.entity.EventEntity
import cz.sobtech.hapBeer.data.entity.KegEntity
import cz.sobtech.hapBeer.data.entity.PersonEntity
import cz.sobtech.hapBeer.data.repository.PivoRepository
import cz.sobtech.hapBeer.ui.util.AppLogger
import cz.sobtech.hapBeer.ui.util.LogLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventDetailViewModel(
    private val repository: PivoRepository,
    private val eventId: Long
) : ViewModel() {

    val event: StateFlow<EventEntity?> = repository.getEvent(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val kegs: StateFlow<List<KegEntity>> = repository.getKegsForEvent(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allPeople: StateFlow<List<PersonEntity>> = repository.allPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** personId → celkový počet piv v akci (přes všechny bečky). */
    val beerCounts: StateFlow<Map<Long, Int>> = repository.getBeerCountsForEvent(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Surové záznamy pro výpočet statistik (timestampy, rozpad po bečkách). */
    val eventRecords: StateFlow<List<BeerRecordEntity>> = repository.getRecordsForEvent(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addKeg(name: String, price: Double, sizeLiters: Double) {
        viewModelScope.launch {
            repository.insertKeg(
                KegEntity(eventId = eventId, name = name, price = price, sizeLiters = sizeLiters)
            )
            AppLogger.log(LogLevel.INFO, "Bečka vytvořena: \"$name\" (akce ID:$eventId)")
        }
    }

    fun deleteKeg(keg: KegEntity) {
        viewModelScope.launch {
            repository.deleteKeg(keg)
            AppLogger.log(LogLevel.INFO, "Bečka smazána: \"${keg.name}\"")
        }
    }

    companion object {
        fun provideFactory(
            repository: PivoRepository,
            eventId: Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { EventDetailViewModel(repository, eventId) }
        }
    }
}
