package me.bluegecko.framelessviewer

import picocli.CommandLine.Command
import picocli.CommandLine.Option

@Command(name = "FramelessViewer", mixinStandardHelpOptions = true)
class ArgumentsParser : Runnable {
    @Option(names = ["-d", "--daemon"], description = ["Start with daemon"])
    var daemon = false

    @Option(names = ["-p", "--path"], description = ["Path of the image to be loaded from the beginning"])
    var initPath = ""

    override fun run() {}
}
