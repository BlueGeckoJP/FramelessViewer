package me.bluegecko.framelessviewer

import picocli.CommandLine.*
import java.io.File
import java.io.FileWriter

@Command(name = "FramelessViewer", mixinStandardHelpOptions = true, subcommands = [DCSubcomand::class])
class ArgumentsParser : Runnable {
    @Option(names = ["-d", "--daemon"], description = ["Start with daemon"])
    var daemon = false

    @Option(names = ["-p", "--path"], description = ["Path of the image to be loaded from the beginning"])
    var initPath = ""

    override fun run() {
    }
}

@Command(
    name = "dc",
    mixinStandardHelpOptions = true,
    description = ["Daemon Client Subcommands (Daemon mode must be enabled)"],
    subcommands = [DCSubcomand.OpenCommand::class]
)
class DCSubcomand : Runnable {
    @Command(name = "open", description = ["Open a file"])
    class OpenCommand : Runnable {
        @Parameters(index = "0", description = ["Path of the file"], paramLabel = "PATH")
        var path = ""

        override fun run() {
            try {
                if (File(pipePath).exists()) {
                    FileWriter(pipePath, true).use { writer ->
                        writer.write("open $path\n")
                        writer.flush()
                        println("DC Open: send command 'open $path'")
                    }
                } else {
                    println("DC Open: pipe not found. Daemon not running?")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            isNormalExecution = false
        }
    }

    override fun run() {
        isNormalExecution = false
    }
}
