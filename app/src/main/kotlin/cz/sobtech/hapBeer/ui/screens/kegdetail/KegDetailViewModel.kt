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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
