package me.bluegecko.framelessviewer

import com.formdev.flatlaf.themes.FlatMacDarkLaf
import me.bluegecko.framelessviewer.ChannelMessage.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.UIManager
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val threadDataList = mutableListOf<ThreadData>()
    var isFirstTime = true

    UIManager.setLookAndFeel(FlatMacDarkLaf())

    while (true) {
        if (isFirstTime) {
            val returnValue = if (args.size == 1) {
                runApp(AppData(initPath = args[0]))
            } else {
                runApp()
            }
            threadDataList.add(returnValue)
            isFirstTime = false
        }

        if (threadDataList.isEmpty()) {
            exitProcess(0)
        }

        val addList = mutableListOf<ThreadData>()

        val iterator = threadDataList.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val message = item.channel.get().message
            when (message) {
                Exit -> {
                    iterator.remove()
                    item.thread.interrupt()
                    println("Exited ${item.uuid}")
                }

                NewWindow -> {
                    val returnValue = runApp()
                    addList.add(returnValue)
                    item.channel.set(Channel())
                    println("New ${item.uuid} -> ${returnValue.uuid}")
                }

                Reinit -> {
                    val returnValue = runApp(item.channel.get().appData)
                    addList.add(returnValue)
                    iterator.remove()
                    item.thread.interrupt()
                    println("Reinit ${item.uuid} -> ${returnValue.uuid}")
                }

                NewWindowWithImage -> {
                    val returnValue = runApp(item.channel.get().appData)
                    addList.add(returnValue)
                    item.channel.set(Channel())
                    println("NewWindowWithImage ${item.uuid} -> ${returnValue.uuid}")
                }

                Normal -> {}
            }
        }
        threadDataList.addAll(addList)
        Thread.sleep(500)
    }
}

fun runApp(initAppData: AppData = AppData()): ThreadData {
    val channel = AtomicReference(Channel(appData = initAppData))
    val thread = Thread { App(channel) }
    thread.start()
    return ThreadData(thread = thread, channel = channel)
}
