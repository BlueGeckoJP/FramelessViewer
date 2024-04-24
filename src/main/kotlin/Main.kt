package me.bluegecko

import java.awt.BorderLayout
import java.awt.GridBagLayout
import java.awt.Image
import java.awt.Rectangle
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import kotlin.system.exitProcess

fun main() {
    var app = App()
    app.isVisible = true
    while (true) {
        if (!app.isVisible) {
            app.dispose()
            app = App()
            app.isVisible = true
        }
    }
}

class App : JFrame() {
    companion object {
        var isUndecoratedWindow = true
        var bounds = Rectangle(0, 0, 600, 400)
        var filePath = ""
        var frameWidth = 0
        var frameHeight = 0
        var fileList = mutableListOf("")
        var fileListIndex = 0
    }
    private var iconLabel: JLabel

    init {
        title = "FramelessViewer"
        defaultCloseOperation = EXIT_ON_CLOSE
        isUndecorated = isUndecoratedWindow
        bounds = App.bounds

        frameWidth = 600 - insets.left - insets.right
        frameHeight = 400 - insets.top - insets.bottom

        val popupMenu = PopupMenu(this)
        val itemTitleBar = JMenuItem("Toggle TitleBar")
        val itemOpen = JMenuItem("Open")
        val itemExit = JMenuItem("Exit")
        itemTitleBar.addActionListener { toggleTitleBar() }
        itemOpen.addActionListener { open() }
        itemExit.addActionListener{ exit() }
        popupMenu.add(itemTitleBar)
        popupMenu.add(itemOpen)
        popupMenu.add(itemExit)

        iconLabel = JLabel()
        iconLabel.horizontalAlignment = JLabel.CENTER
        iconLabel.verticalAlignment = JLabel.CENTER

        val panel = JPanel()
        panel.layout = GridBagLayout()
        panel.add(iconLabel)

        contentPane.add(panel, BorderLayout.CENTER)

        addComponentListener(WindowResizeListener())
        addKeyListener(ArrowKeyListener())
    }

    inner class WindowResizeListener : ComponentListener {
        override fun componentHidden(p0: ComponentEvent?) {}
        override fun componentMoved(p0: ComponentEvent?) {}
        override fun componentShown(p0: ComponentEvent?) {}

        override fun componentResized(p0: ComponentEvent?) {
            frameWidth = size.width - insets.left - insets.right
            frameHeight = size.height - insets.top - insets.bottom

            this@App.updateImage()
        }
    }

    inner class ArrowKeyListener : KeyListener {
        override fun keyPressed(p0: KeyEvent?) {}
        override fun keyTyped(p0: KeyEvent?) {}
        override fun keyReleased(p0: KeyEvent?) {
            if (p0 != null && filePath != "") {
                if (p0.keyCode == 37 || p0.keyCode == 39) { // Left arrow key: 37 | Right arrow key: 39
                    val filePathSlashIndex = filePath.lastIndexOf("/")
                    val dirPath = filePath.substring(0, filePathSlashIndex)
                    val dir = File(dirPath)
                    val rawFileList = dir.listFiles()
                    val filenameList = rawFileList?.filter { it.isFile }?.map { it.absolutePath.toString() }?.filter { it.contains(Regex(".jpg|.jpeg|.png")) }
                        ?.sorted()
                    fileList = filenameList as MutableList<String>
                    fileListIndex = fileList.indexOf(filePath)
                    if (p0.keyCode == 37) { // Left arrow key
                        if (fileListIndex - 1 < 0) {
                            fileListIndex = fileList.size - 1
                        } else {
                            fileListIndex -= 1
                        }
                        filePath = fileList[fileListIndex]
                        updateImage()
                    } else if (p0.keyCode == 39) { // Right arrow key
                        if (fileListIndex + 1 >= fileList.size) {
                            fileListIndex = 0
                        } else {
                            fileListIndex += 1
                        }
                        filePath = fileList[fileListIndex]
                        updateImage()
                    }
                }
            }
        }
    }

    private fun toggleTitleBar() {
        isUndecoratedWindow = !isUndecoratedWindow
        App.bounds = bounds
        this@App.isVisible = false
    }

    private fun open() {
        val chooser = JFileChooser()
        chooser.showOpenDialog(null)
        val file = chooser.selectedFile
        if (file != null) {
            filePath = file.absolutePath
            updateImage()
        }
    }

    private fun exit() {
        exitProcess(0)
    }

    private fun updateImage() {
        try {
            val file = File(filePath)
            val bufferedImage = ImageIO.read(file)

            var width = this@App.width
            var height = this@App.height
            if (!isUndecoratedWindow) {
                width = frameWidth
                height = frameHeight
            }

            if (bufferedImage.width > width || bufferedImage.height > height) {
                val widthStandardSize = Pair(width, getSizeFromAspectRatio(width, getAspectRatio(bufferedImage.width, bufferedImage.height), false))
                val heightStandardSize = Pair(getSizeFromAspectRatio(height, getAspectRatio(bufferedImage.width, bufferedImage.height), true), height)

                if (width >= widthStandardSize.first && height >= widthStandardSize.second) {
                    val image = bufferedImage.getScaledInstance(widthStandardSize.first, widthStandardSize.second, Image.SCALE_SMOOTH)
                    iconLabel.icon = ImageIcon(image)
                } else if (width >= heightStandardSize.first && height >= heightStandardSize.second) {
                    val image = bufferedImage.getScaledInstance(heightStandardSize.first, heightStandardSize.second, Image.SCALE_SMOOTH)
                    iconLabel.icon = ImageIcon(image)
                }
            }
        } catch (_: IIOException) {
            iconLabel.text = ""
        }
    }

    // Example: firstI1: 1920, firstI2: 1080 = Pair<16, 9>
    private fun getAspectRatio(firstI1: Int, firstI2: Int): Pair<Int, Int> {
        var i1 = firstI1
        var i2 = firstI2
        while (true) {
            val ans = i1 % i2
            if (ans == 0) {
                return firstI1 / i2 to firstI2 / i2
            } else {
                i1 = i2
                i2 = ans
                continue
            }
        }
    }

    // Example: size: 1920, i: Pair<16, 9>, isHeightStandard = true/false = 1080
    private fun getSizeFromAspectRatio(size: Int, i: Pair<Int, Int>, isHeightStandard: Boolean): Int {
        if (isHeightStandard) {
            return (size * i.first) / i.second
        }
        return (size * i.second) / i.first
    }
}
