package cz.sobtech.hapBeer.ui.screens.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.sobtech.hapBeer.data.entity.PersonEntity
import cz.sobtech.hapBeer.data.repository.PivoRepository
import cz.sobtech.hapBeer.ui.util.AppLogger
import cz.sobtech.hapBeer.ui.util.LogLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeopleViewModel(private val repository: PivoRepository) : ViewModel() {

    val people: StateFlow<List<PersonEntity>> = repository.allPeople
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPerson(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.insertPerson(PersonEntity(name = trimmed))
            AppLogger.log(LogLevel.INFO, "Osoba přidána: \"$trimmed\"")
        }
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch {
            repository.deletePerson(person)
            AppLogger.log(LogLevel.INFO, "Osoba smazána: \"${person.name}\"")
        }
    }

    companion object {
        fun provideFactory(repository: PivoRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { PeopleViewModel(repository) } }
    }
}
