package me.bluegecko

import java.awt.BorderLayout
import java.awt.GridBagLayout
import java.awt.Image
import java.awt.Rectangle
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel

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
    }
    private var iconLabel: JLabel

    init {
        title = "FramelessViewer"
        defaultCloseOperation = EXIT_ON_CLOSE
        isUndecorated = App.isUndecoratedWindow
        bounds = App.bounds

        frameWidth = 600 - insets.left - insets.right
        frameHeight = 400 - insets.top - insets.bottom

        val popupMenu = PopupMenu(this)
        val itemTitleBar = JMenuItem("Toggle TitleBar")
        val itemOpen = JMenuItem("Open")
        itemTitleBar.addActionListener { toggleTitleBar() }
        itemOpen.addActionListener { open() }
        popupMenu.add(itemTitleBar)
        popupMenu.add(itemOpen)

        iconLabel = JLabel()
        iconLabel.horizontalAlignment = JLabel.CENTER
        iconLabel.verticalAlignment = JLabel.CENTER

        val panel = JPanel()
        panel.layout = GridBagLayout()
        panel.add(iconLabel)

        contentPane.add(panel, BorderLayout.CENTER)

        addComponentListener(WindowResizeListener())
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

    private fun toggleTitleBar() {
        App.isUndecoratedWindow = !App.isUndecoratedWindow
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

    private fun updateImage() {
        try {
            val file = File(filePath)
            val bufferedImage = ImageIO.read(file)

            var size1 = this@App.width; var size2 = this@App.height
            val imgSize1 = bufferedImage.width; val imgSize2 = bufferedImage.height
            if (!isUndecoratedWindow) {
                size1 = frameWidth; size2 = frameHeight
            }
            if (size1 - getSizeFromAR(size2, getAspectRatio(bufferedImage.width, bufferedImage.height)) < size2 - getSizeFromAR(
                    size1, getAspectRatio(bufferedImage.width, bufferedImage.height))) {
                val (size1, size2) = Pair(size2, size1)
                val (imgSize1, imgSize2) = Pair(imgSize2, imgSize1)
            }
            if (imgSize1 > size1) {
                setImage(bufferedImage, size1)
            } else if (imgSize2 > size2) {
                setImage(bufferedImage, size2)
            } else {
                iconLabel.icon = ImageIcon(bufferedImage)
            }
        } catch (_: IIOException) {
            iconLabel.text = ""
        }
    }

    private fun setImage(bufferedImage: BufferedImage, size: Int) {
        val image = bufferedImage.getScaledInstance(size, getSizeFromAR(size, getAspectRatio(bufferedImage.width, bufferedImage.height)) , Image.SCALE_SMOOTH)
        iconLabel.icon = ImageIcon(image)
    }

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

    private fun getSizeFromAR(size: Int, i: Pair<Int, Int>): Int {
        return (size * i.second) / i.first
    }
}
