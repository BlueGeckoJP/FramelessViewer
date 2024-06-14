package me.bluegecko

import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

fun main() {
    val channelList = mutableListOf<AtomicReference<Channel>>()
    val channelList2 = mutableListOf<AtomicReference<Channel>>()
    var isFirstTime = true
    while (true) {
        if (isFirstTime) {
            val msg = runApp()
            channelList2.add(msg)
            isFirstTime = false
            channelList.addAll(channelList2)
            channelList2.clear()
        }

        if (channelList.isEmpty()) {
            exitProcess(0)
        }

        val iter = channelList.iterator()
        while (iter.hasNext()) {
            val element = iter.next()
            if (element.get().message == ChannelMessage.Exit) {
                iter.remove()
            } else if (element.get().message == ChannelMessage.NewWindow) {
                val msg = runApp()
                channelList2.add(msg)
            } else if (element.get().message == ChannelMessage.Reinit) {
                val msg = runApp(element.get().initAppData)
                channelList2.add(msg)
                iter.remove()
            }
        }

        channelList.addAll(channelList2)
        channelList2.clear()

        println(channelList)
        Thread.sleep(1000)
    }
}

fun runApp(initAppData: AppData = AppData()): AtomicReference<Channel> {
    val msg = AtomicReference(Channel(ChannelMessage.Normal, initAppData))
    val thread = Thread {
        val app = App(msg)
        app.isVisible = true
    }
    thread.start()
    return msg
}