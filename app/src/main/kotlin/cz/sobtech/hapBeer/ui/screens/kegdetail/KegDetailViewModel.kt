package cz.sobtech.hapBeer.ui.screens.kegdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.sobtech.hapBeer.data.entity.BeerRecordEntity
import cz.sobtech.hapBeer.data.entity.KegEntity
import cz.sobtech.hapBeer.data.entity.PersonEntity
import cz.sobtech.hapBeer.data.repository.PivoRepository
import cz.sobtech.hapBeer.ui.util.KegConsumptionStats
import cz.sobtech.hapBeer.ui.util.computeKegConsumptionStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KegDetailViewModel(
    private val repository: PivoRepository,
    val kegId: Long
) : ViewModel() {

    val keg: StateFlow<KegEntity?> = repository.getKeg(kegId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allPeople: StateFlow<List<PersonEntity>> = repository.allPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** personId → počet piv z TÉTO bečky. */
    val beerCountsByPerson: StateFlow<Map<Long, Int>> = repository.getBeerCountsForKeg(kegId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ── Event-level data pro PersonDetailDialog ───────────────────────────────

    private val kegFlow: Flow<KegEntity> = repository.getKeg(kegId).filterNotNull()

    @OptIn(ExperimentalCoroutinesApi::class)
    val eventRecords: StateFlow<List<BeerRecordEntity>> = kegFlow
        .flatMapLatest { k -> repository.getRecordsForEvent(k.eventId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val eventKegs: StateFlow<List<KegEntity>> = kegFlow
        .flatMapLatest { k -> repository.getKegsForEvent(k.eventId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val eventBeerCounts: StateFlow<Map<Long, Int>> = kegFlow
        .flatMapLatest { k -> repository.getBeerCountsForEvent(k.eventId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ── Statistiky spotřeby bečky ─────────────────────────────────────────────

    private val tickerFlow: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }

    val consumptionStats: StateFlow<KegConsumptionStats?> = combine(
        kegFlow,
        repository.getRecordsForKeg(kegId),
        tickerFlow
    ) { keg, records, _ ->
        computeKegConsumptionStats(records, keg.sizeLiters, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Akce ─────────────────────────────────────────────────────────────────

    fun recordBeer(personId: Long) {
        viewModelScope.launch {
            repository.insertBeerRecord(
                BeerRecordEntity(
                    kegId = kegId,
                    personId = personId,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun undoLastBeer(personId: Long) {
        viewModelScope.launch {
            repository.deleteLastBeerRecordForPersonInKeg(kegId, personId)
        }
    }

    companion object {
        fun provideFactory(
            repository: PivoRepository,
            kegId: Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { KegDetailViewModel(repository, kegId) }
        }
    }
}
