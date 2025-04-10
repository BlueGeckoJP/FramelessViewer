package me.bluegecko.framelessviewer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

const val pipePath = "/tmp/framelessviewer_pipe"

class Daemon {
    @Volatile
    private var isRunning = false
    private var serverThread: Job? = null
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        if (File(pipePath).exists()) {
            File(pipePath).delete()
        }
        Runtime.getRuntime().exec(arrayOf("mkfifo", pipePath)).waitFor()
    }

    fun start() {
        isRunning = true

        serverThread = CoroutineScope(Dispatchers.IO).launch {
            runServer()
        }
    }

    fun stop() {
        isRunning = false
        serverThread?.cancel()

        File(pipePath).delete()
    }

    private fun runServer() {
        while (isRunning) {
            try {
                BufferedReader(FileReader(pipePath)).use { reader ->
                    var line = ""
                    while (isRunning && reader.readLine().also { line = it } != null) {
                        if (line.startsWith("open ")) {
                            val path = line.substring("open ".length)
                            if (File(path).exists()) {
                                appController.newWindowByDaemon(path)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e.message != null) {
                    logger.error("Daemon: Error reading from pipe: ${e.message}")
                    Thread.sleep(1000)
                }
            }
        }
    }
}