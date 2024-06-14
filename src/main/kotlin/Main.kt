package me.bluegecko

import me.bluegecko.ChannelMessage.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val channelMap = mutableMapOf<String, Pair<Thread, AtomicReference<Channel>>>()
    var isFirstTime = true

    while (true) {
        if (isFirstTime) {
            val returnValue = if (args.size == 1) {
                runApp(AppData(filePath = args[0]))
            } else {
                runApp()
            }
            channelMap[returnValue.first] = returnValue.second
            isFirstTime = false
        }
        
        if (channelMap.isEmpty()) {
            exitProcess(0)
        }

        val iter = channelMap.iterator()
        while (iter.hasNext()) {
            val (k, v) = iter.next()
            val message = v.second.get().message
            when (message) {
                Exit -> {
                    iter.remove()
                    v.first.interrupt()
                }

                NewWindow -> {
                    v.second.set(Channel(Normal, AppData()))
                    val returnValue = runApp()
                    channelMap[returnValue.first] = returnValue.second
                }

                Reinit -> {
                    val returnValue = runApp(v.second.get().initAppData)
                    channelMap[returnValue.first] = returnValue.second
                    channelMap.remove(k)
                    v.first.interrupt()
                }

                Normal -> {}
            }
        }

        Thread.sleep(500)
    }
}

fun runApp(initAppData: AppData = AppData()): Pair<String, Pair<Thread, AtomicReference<Channel>>> {
    val channel = AtomicReference(Channel(Normal, initAppData))
    val thread = Thread {
        val app = App(channel)
        app.isVisible = true
    }
    thread.start()
    return UUID.randomUUID().toString() to (thread to channel)
}
