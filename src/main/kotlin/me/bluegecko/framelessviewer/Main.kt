package me.bluegecko.framelessviewer

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.File
import javax.imageio.ImageIO
import javax.swing.UIManager

lateinit var appController: AppController
var daemon: Daemon? = null

fun main(args: Array<String>) = runBlocking {
    setupLogger()

    val logger = LoggerFactory.getLogger(this::class.java)

    UIManager.setLookAndFeel(FlatMacDarkLaf())
    ImageIO.scanForPlugins()
    ImageIO.getImageReadersByFormatName("webp").next()

    appController = AppController()

    val argumentsParser = ArgumentsParser()
    CommandLine(argumentsParser).execute(*args)

    Runtime.getRuntime().addShutdownHook(Thread {
        if (daemon != null) {
            daemon!!.stop()
        }

        appController.threadDataList.forEach { it.thread.cancel() }
    })

    if (argumentsParser.daemon) {
        logger.info("Enabled daemon mode")
        daemon = Daemon()
        daemon!!.start()
    }

    appController.run(argumentsParser.initPath)
}

fun setupLogger() {
    val saveDir = File(System.getProperty("user.home"), "/.framelessviewer")
    if (!saveDir.exists()) {
        saveDir.mkdir()
    }

    val logFile = saveDir.resolve("latest.log")
    if (logFile.exists()) {
        logFile.delete()
    }

    val context = LoggerFactory.getILoggerFactory() as LoggerContext

    val encoder = PatternLayoutEncoder()
    encoder.context = context
    encoder.pattern = "[%d{HH:mm:ss.SSS}] [%thread] %-5level %logger{36} - %msg%n"
    encoder.start()

    val fileAppender = FileAppender<ILoggingEvent>()
    fileAppender.context = context
    fileAppender.name = "FILE"
    fileAppender.file = saveDir.resolve("latest.log").path
    fileAppender.encoder = encoder
    fileAppender.start()

    val consoleAppender = ConsoleAppender<ILoggingEvent>()
    consoleAppender.context = context
    consoleAppender.name = "CONSOLE"
    consoleAppender.encoder = encoder
    consoleAppender.start()

    val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
    rootLogger.detachAndStopAllAppenders()
    rootLogger.addAppender(fileAppender)
    rootLogger.addAppender(consoleAppender)
    rootLogger.level = Level.DEBUG
}