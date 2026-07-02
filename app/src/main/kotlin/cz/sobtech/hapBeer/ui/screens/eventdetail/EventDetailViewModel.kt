package cz.sobtech.hapBeer.ui.screens.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.sobtech.hapBeer.data.entity.EventEntity
import cz.sobtech.hapBeer.data.entity.KegEntity
import cz.sobtech.hapBeer.data.entity.PersonEntity
import cz.sobtech.hapBeer.data.repository.PivoRepository
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

    /** Všichni globální lidé – pro EventSummaryBottomSheet (překlad personId → jméno). */
    val allPeople: StateFlow<List<PersonEntity>> = repository.allPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** personId → celkový počet piv v akci (přes všechny bečky) – pro EventSummaryBottomSheet. */
    val beerCounts: StateFlow<Map<Long, Int>> = repository.getBeerCountsForEvent(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun addKeg(name: String, price: Double, sizeLiters: Double) {
        viewModelScope.launch {
            repository.insertKeg(
                KegEntity(eventId = eventId, name = name, price = price, sizeLiters = sizeLiters)
            )
        }
    }

    fun deleteKeg(keg: KegEntity) {
        viewModelScope.launch { repository.deleteKeg(keg) }
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
