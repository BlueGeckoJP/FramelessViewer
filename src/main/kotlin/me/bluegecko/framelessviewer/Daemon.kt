package me.bluegecko.framelessviewer

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

const val pipePath = "/tmp/framelessviewer_pipe"

class Daemon {
    @Volatile
    private var isRunning = false
    private var serverThread: Thread? = null

    init {
        if (File(pipePath).exists()) {
            File(pipePath).delete()
        }
        Runtime.getRuntime().exec("mkfifo $pipePath").waitFor()
    }

    fun start() {
        isRunning = true

        serverThread = Thread {
            runServer()
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        isRunning = false
        serverThread?.let { thread ->
            try {
                thread.join(5000)
            } catch (e: InterruptedException) {
                println("Daemon: Shutdown interrupted")
            }
        }

        File(pipePath).delete()
    }

    private fun runServer() {
        while (isRunning) {
            try {
                BufferedReader(FileReader(pipePath)).use { reader ->
                    println("Daemon: Waiting for commands...")

                    var line = ""
                    while (isRunning && reader.readLine().also { line = it } != null) {
                        println(line)
                    }
                }
            } catch (e: Exception) {
                if (e.message != null) {
                    println("Daemon: Error reading from pipe: ${e.message}")
                    Thread.sleep(1000)
                }
            }
        }
    }
}