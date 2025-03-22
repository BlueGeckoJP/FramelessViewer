package me.bluegecko.framelessviewer

import com.formdev.flatlaf.themes.FlatMacDarkLaf
import kotlinx.coroutines.runBlocking
import picocli.CommandLine
import javax.imageio.ImageIO
import javax.swing.UIManager

lateinit var appController: AppController
var daemon: Daemon? = null

fun main(args: Array<String>) = runBlocking {
    UIManager.setLookAndFeel(FlatMacDarkLaf())
    ImageIO.scanForPlugins()
    ImageIO.getImageReadersByFormatName("webp").next()

    val argumentsParser = ArgumentsParser()
    CommandLine(argumentsParser).execute(*args)

    Runtime.getRuntime().addShutdownHook(Thread {
        if (daemon != null) {
            daemon!!.stop()
        }

        appController.threadDataList.forEach { it.thread.cancel() }
    })

    if (argumentsParser.daemon) {
        println("Daemon Mode!")
        daemon = Daemon()
        daemon!!.start()
    }

    appController = AppController()
    appController.run(argumentsParser.initPath)
}