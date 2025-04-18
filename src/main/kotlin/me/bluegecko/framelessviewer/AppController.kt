package me.bluegecko.framelessviewer

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import me.bluegecko.framelessviewer.data.AppData
import me.bluegecko.framelessviewer.data.Channel
import me.bluegecko.framelessviewer.data.ChannelMessage
import me.bluegecko.framelessviewer.data.ChannelMessage.*
import me.bluegecko.framelessviewer.data.ThreadData
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

class AppController {
    val threadDataList = mutableListOf<ThreadData>()
    private var isNormalExecution = true
    private var isFirstTime = true
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val scope = CoroutineScope(Dispatchers.Swing + SupervisorJob())

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
                        logger.info("Exited ${item.uuid}")
                    }

                    NewWindow -> {
                        val returnValue = runApp()
                        addList.add(returnValue)
                        item.channel.set(Channel())
                        logger.info("New ${item.uuid} -> ${returnValue.uuid}")
                    }

                    Reinit -> {
                        val returnValue = runApp(AppData(item.appData.get().copy()))
                        addList.add(returnValue)
                        iterator.remove()
                        item.thread.cancel()
                        logger.info("Reinit ${item.uuid} -> ${returnValue.uuid}")
                    }

                    NewWindowWithImage -> {
                        val returnValue = runApp(AppData(item.appData.get().copy()))
                        addList.add(returnValue)
                        item.channel.set(Channel())
                        logger.info("NewWindowWithImage ${item.uuid} -> ${returnValue.uuid}")
                    }

                    SendImage -> {
                        try {
                            val itemChannel = item.channel.get()
                            val targetThreadData = threadDataList.find { it.uuid == itemChannel.sendImageTo }
                                ?: throw IllegalStateException("Target window not found: ${itemChannel.sendImageTo}")
                            val targetChannel = targetThreadData.channel.get()
                            targetChannel.receivedImagePath = itemChannel.sendImagePath
                            targetChannel.isReceived = true
                            item.channel.set(Channel())
                            logger.info("SendImage ${item.uuid} -> ${targetThreadData.uuid}")
                        } catch (e: Exception) {
                            logger.error("Failed to send image", e)
                            item.channel.set(Channel()) // Reset channel state on error
                        }
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

        val thread = scope.launch {
            try {
                App(channel, uuid, initAppData, initPath)
            } catch (e: Exception) {
                logger.error("Failed to create application window", e)
                channel.set(Channel(Exit)) // Signal thread to exit on error
                throw e
            }
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
        try {
            if (threadDataList.isEmpty()) {
                throw IllegalStateException("No existing windows to copy settings from")
            }

            val lastAppData = threadDataList.last().appData.get()
            val returnValue = runApp(AppData(lastAppData), path)
            threadDataList.add(returnValue)
            logger.info("NewWindow By Daemon -> ${returnValue.uuid}")
        } catch (e: Exception) {
            logger.error("Failed to create new window by daemon", e)
            throw e
        }
    }

    fun stop() {
        isNormalExecution = false
        scope.cancel()
    }
}
