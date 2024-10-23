package me.bluegecko.framelessviewer

import com.formdev.flatlaf.themes.FlatMacDarkLaf
import me.bluegecko.framelessviewer.ChannelMessage.*
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.UIManager
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val channelMap = mutableMapOf<String, Pair<Thread, AtomicReference<Channel>>>()
    var isFirstTime = true

    UIManager.setLookAndFeel(FlatMacDarkLaf())

    while (true) {
        if (isFirstTime) {
            val returnValue = if (args.size == 1) {
                runApp(AppData(initPath = args[0]))
            } else {
                runApp()
            }
            channelMap[returnValue.first] = returnValue.second
            isFirstTime = false
        }

        if (channelMap.isEmpty()) {
            exitProcess(0)
        }

        val channelMapAdd = mutableMapOf<String, Pair<Thread, AtomicReference<Channel>>>()

        val iter = channelMap.iterator()
        while (iter.hasNext()) {
            val (k, v) = iter.next()
            val message = v.second.get().message
            when (message) {
                Exit -> {
                    iter.remove()
                    v.first.interrupt()
                    println("Exited $k")
                }

                NewWindow -> {
                    val returnValue = runApp()
                    channelMapAdd[returnValue.first] = returnValue.second
                    v.second.set(Channel(Normal, AppData()))
                    println("New $k -> ${returnValue.first}")
                }

                Reinit -> {
                    val returnValue = runApp(v.second.get().initAppData)
                    channelMapAdd[returnValue.first] = returnValue.second
                    iter.remove()
                    v.first.interrupt()
                    println("Reinit $k -> ${returnValue.first}")
                }

                NewWindowWithImage -> {
                    val returnValue = runApp(v.second.get().initAppData)
                    channelMapAdd[returnValue.first] = returnValue.second
                    v.second.set(Channel(Normal, AppData()))
                    println("NewWindowWithImage $k -> ${returnValue.first}")
                }

                Normal -> {}
            }
        }

        for (item in channelMapAdd) {
            channelMap[item.key] = item.value
        }

        Thread.sleep(500)
    }
}

fun runApp(initAppData: AppData = AppData()): Pair<String, Pair<Thread, AtomicReference<Channel>>> {
    val channel = AtomicReference(Channel(Normal, initAppData))
    val thread = Thread {
        App(channel)
    }
    thread.start()
    return UUID.randomUUID().toString() to (thread to channel)
}
