package cz.sobtech.hapBeer.data

import android.app.Application
import cz.sobtech.hapBeer.data.database.AppDatabase
import cz.sobtech.hapBeer.data.repository.PivoRepository

class AppContainer(application: Application) {
    val database: AppDatabase by lazy { AppDatabase.getInstance(application) }

    val pivoRepository: PivoRepository by lazy {
        PivoRepository(
            eventDao = database.eventDao(),
            personDao = database.personDao(),
            kegDao = database.kegDao(),
            beerRecordDao = database.beerRecordDao()
        )
    }
}
