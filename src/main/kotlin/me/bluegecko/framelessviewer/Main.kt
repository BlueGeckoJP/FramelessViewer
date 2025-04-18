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

    try {
        UIManager.setLookAndFeel(FlatMacDarkLaf())
        ImageIO.scanForPlugins()

        // WebP support check
        val webpReaders = ImageIO.getImageReadersByFormatName("webp")
        if (!webpReaders.hasNext()) {
            logger.error("WebP format is not supported")
            throw RuntimeException("WebP format is not supported")
        }
        webpReaders.next()

        appController = AppController()

        val argumentsParser = ArgumentsParser()
        CommandLine(argumentsParser).execute(*args)

        // Setup shutdown hook for proper resource cleanup
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                // Clean up daemon resources
                daemon?.let {
                    it.stop()
                }

                // Cancel all threads
                appController.threadDataList.forEach {
                    try {
                        it.thread.cancel()
                    } catch (e: Exception) {
                        logger.error("Error canceling thread: ${it.uuid}", e)
                    }
                }

                // Clean up image resources
                try {
                    ImageIO.setUseCache(false)

                    Runtime.getRuntime().gc()
                } catch (e: Exception) {
                    logger.error("Error cleaning up image resources", e)
                }
            } catch (e: Exception) {
                logger.error("Error during shutdown", e)
            }
        })

        if (argumentsParser.daemon) {
            logger.info("Enabled daemon mode")
            daemon = Daemon()
            daemon?.start()
        }

        appController.run(argumentsParser.initPath)
    } catch (e: Exception) {
        logger.error("Application startup failed", e)
        throw e
    }
}

fun setupLogger() {
    try {
        val saveDir = File(System.getProperty("user.home"), "/.framelessviewer")
        if (!saveDir.exists() && !saveDir.mkdir()) {
            throw RuntimeException("Failed to create log directory: ${saveDir.absolutePath}")
        }

        val context = LoggerFactory.getILoggerFactory() as LoggerContext

        val encoder = PatternLayoutEncoder().apply {
            this.context = context
            pattern = "[%d{HH:mm:ss.SSS}] [%thread] %-5level %logger{36} - %msg%n"
            start()
        }

        val fileAppender = FileAppender<ILoggingEvent>().apply {
            this.context = context
            name = "FILE"
            file = saveDir.resolve("latest.log").path
            this.encoder = encoder
            start()
        }

        val consoleAppender = ConsoleAppender<ILoggingEvent>().apply {
            this.context = context
            name = "CONSOLE"
            this.encoder = encoder
            start()
        }

        context.getLogger(Logger.ROOT_LOGGER_NAME).apply {
            detachAndStopAllAppenders()
            addAppender(fileAppender)
            addAppender(consoleAppender)
            level = Level.DEBUG
        }
    } catch (e: Exception) {
        System.err.println("Failed to setup logger: ${e.message}")
        throw e
    }
}