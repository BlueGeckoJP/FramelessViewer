package me.bluegecko.framelessviewer

import picocli.CommandLine.*

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

        override fun run() {}
    }

    override fun run() {}
}
