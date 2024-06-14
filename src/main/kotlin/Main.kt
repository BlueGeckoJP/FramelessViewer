package me.bluegecko

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

fun main() {
    val channelMap = mutableMapOf<String, AtomicReference<Channel>>()
    var isFirstTime = true
    while (true) {
        if (isFirstTime) {
            val returnValue = runApp()
            channelMap[returnValue.first] = returnValue.second
            isFirstTime = false
        }

        if (channelMap.isEmpty()) {
            exitProcess(0)
        }

        val tempChannelMap = mutableMapOf<String, AtomicReference<Channel>>()
        tempChannelMap.putAll(channelMap)
        tempChannelMap.forEach { (k, v) ->
            run {
                if (v.get().message == ChannelMessage.Exit) {
                    channelMap.remove(k)
                } else if (v.get().message == ChannelMessage.NewWindow) {
                    v.set(Channel(ChannelMessage.Normal, AppData()))
                    val returnValue = runApp()
                    channelMap[returnValue.first] = returnValue.second
                } else if (v.get().message == ChannelMessage.Reinit) {
                    val returnValue = runApp(v.get().initAppData)
                    channelMap[returnValue.first] = returnValue.second
                    channelMap.remove(k)
                }
            }
        }

        Thread.sleep(1000)
    }
}

fun runApp(initAppData: AppData = AppData()): Pair<String, AtomicReference<Channel>> {
    val channel = AtomicReference(Channel(ChannelMessage.Normal, initAppData))
    val thread = Thread {
        val app = App(channel)
        app.isVisible = true
    }
    thread.start()
    return UUID.randomUUID().toString() to channel
}