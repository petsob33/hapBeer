package cz.sobtech.hapBeer

import android.app.Application
import cz.sobtech.hapBeer.data.AppContainer
import cz.sobtech.hapBeer.ui.util.AppLogger
import cz.sobtech.hapBeer.ui.util.LogLevel

class HapBeerApp : Application() {

    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLogger.logSync(
                LogLevel.ERROR,
                "CRASH [${thread.name}]: ${throwable.message}\n${throwable.stackTraceToString()}"
            )
            defaultHandler?.uncaughtException(thread, throwable)
        }

        appContainer = AppContainer(this)
        AppLogger.log(LogLevel.INFO, "Aplikace spuštěna")
    }
}
