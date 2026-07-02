package cz.sobtech.hapBeer.ui.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel { INFO, WARN, ERROR }

object AppLogger {

    private val ioDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private const val MAX_BYTES = 1_000_000L

    private lateinit var logFile: File

    fun init(context: Context) {
        logFile = File(context.filesDir, "app.log")
    }

    fun log(level: LogLevel, message: String) {
        if (!::logFile.isInitialized) return
        scope.launch { writeEntry(level, message) }
    }

    /** Synchronní zápis pro crash handler – nepotřebuje coroutine. */
    fun logSync(level: LogLevel, message: String) {
        if (!::logFile.isInitialized) return
        try { writeEntry(level, message) } catch (_: Exception) {}
    }

    suspend fun clearLog() = withContext(ioDispatcher) {
        try { if (::logFile.isInitialized) logFile.writeText("") } catch (_: Exception) {}
    }

    fun getLogFile(): File {
        check(::logFile.isInitialized) { "AppLogger not initialized" }
        return logFile
    }

    fun getLogSize(): Long =
        if (::logFile.isInitialized && logFile.exists()) logFile.length() else 0L

    fun getLogLineCount(): Int {
        if (!::logFile.isInitialized || !logFile.exists()) return 0
        return try { logFile.useLines { it.count() } } catch (_: Exception) { 0 }
    }

    private fun writeEntry(level: LogLevel, message: String) {
        try {
            rotateIfNeeded()
            logFile.appendText("${dateFmt.format(Date())} | ${level.name} | $message\n")
        } catch (_: Exception) {}
    }

    private fun rotateIfNeeded() {
        if (!logFile.exists() || logFile.length() < MAX_BYTES) return
        val lines = logFile.readLines()
        logFile.writeText(lines.drop(lines.size / 2).joinToString("\n") + "\n")
    }
}
