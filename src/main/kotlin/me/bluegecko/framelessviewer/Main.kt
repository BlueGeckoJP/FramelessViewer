package me.bluegecko.framelessviewer

import com.formdev.flatlaf.themes.FlatMacDarkLaf
import me.bluegecko.framelessviewer.data.AppData
import me.bluegecko.framelessviewer.data.Channel
import me.bluegecko.framelessviewer.data.ChannelMessage.*
import me.bluegecko.framelessviewer.data.ThreadData
import picocli.CommandLine
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import javax.swing.UIManager
import kotlin.system.exitProcess

val threadDataList = mutableListOf<ThreadData>()
var isFirstTime = true
var daemon: Daemon? = null

fun main(args: Array<String>) {
    UIManager.setLookAndFeel(FlatMacDarkLaf())
    ImageIO.scanForPlugins()
    ImageIO.getImageReadersByFormatName("webp").next()

    val argumentsParser = ArgumentsParser()
    CommandLine(argumentsParser).execute(*args)

    Runtime.getRuntime().addShutdownHook(Thread {
        if (daemon != null) {
            daemon!!.stop()
        }
    })

    if (argumentsParser.daemon) {
        println("Daemon Mode!")
        daemon = Daemon()
        daemon!!.start()
    }


    while (true) {
        if (isFirstTime) {
            val returnValue = if (argumentsParser.initPath != "") {
                runApp(AppData(initPath = argumentsParser.initPath))
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

                SendImage -> {
                    val itemChannel = item.channel.get()
                    val targetThreadData = threadDataList.filter { it.uuid == itemChannel.sendImageTo }[0]
                    val targetChannel = targetThreadData.channel.get()
                    targetChannel.receivedImagePath = itemChannel.sendImagePath
                    targetChannel.isReceived = true
                    item.channel.set(Channel())
                    println("SendImage ${item.uuid} -> ${targetThreadData.uuid}")
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
    val uuid = UUID.randomUUID().toString()
    val thread = Thread { App(channel, uuid) }
    thread.start()
    return ThreadData(uuid, thread, channel)
}

fun getThreadUUIDs(): List<String> {
    return threadDataList.map { it.uuid }
}

fun newWindowByDaemon(path: String) {
    val returnValue = runApp(AppData(initPath = path))
    threadDataList.add(returnValue)
    println("NewWindow By Daemon  -> ${returnValue.uuid}")
}