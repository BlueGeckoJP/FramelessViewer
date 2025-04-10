package me.bluegecko.framelessviewer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.bluegecko.framelessviewer.data.AppData
import me.bluegecko.framelessviewer.data.Channel
import me.bluegecko.framelessviewer.data.ChannelMessage.*
import me.bluegecko.framelessviewer.data.ThreadData
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

class AppController {
    val threadDataList = mutableListOf<ThreadData>()
    private var isNormalExecution = true
    private var isFirstTime = true

    fun run(initPath: String = "") {
        while (isNormalExecution) {
            if (isFirstTime) {
                val returnValue = if (initPath == "") {
                    runApp()
                } else {
                    runApp(initPath = initPath)
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
                        item.thread.cancel()
                        println("Exited ${item.uuid}")
                    }

                    NewWindow -> {
                        val returnValue = runApp()
                        addList.add(returnValue)
                        item.channel.set(Channel())
                        println("New ${item.uuid} -> ${returnValue.uuid}")
                    }

                    Reinit -> {
                        val returnValue = runApp(AppData(item.appData.get().copy()))
                        addList.add(returnValue)
                        iterator.remove()
                        item.thread.cancel()
                        println("Reinit ${item.uuid} -> ${returnValue.uuid}")
                    }

                    NewWindowWithImage -> {
                        val returnValue = runApp(AppData(item.appData.get().copy()))
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

    private fun runApp(initAppData: AppData = AppData(), initPath: String = ""): ThreadData {
        val channel = AtomicReference(Channel())
        val uuid = UUID.randomUUID().toString()
        val thread = CoroutineScope(Dispatchers.Default).launch {
            App(channel, uuid, initAppData, initPath)
        }
        return ThreadData(uuid, thread, channel, initAppData)
    }

    fun getThreadUUIDs(): List<String> {
        return threadDataList.map { it.uuid }
    }

    fun getShortUUID(uuid: String): String {
        return uuid.substringBefore("-")
    }

    fun newWindowByDaemon(path: String) {
        val lastAppData = threadDataList.last().appData.get()
        val returnValue = runApp(AppData(lastAppData), path)
        threadDataList.add(returnValue)
        println("NewWindow By Daemon -> ${returnValue.uuid}")
    }

    fun stop() {
        isNormalExecution = false
    }
}
