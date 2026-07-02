package cz.sobtech.hapBeer

import android.app.Application
import cz.sobtech.hapBeer.data.AppContainer

class HapBeerApp : Application() {

    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
