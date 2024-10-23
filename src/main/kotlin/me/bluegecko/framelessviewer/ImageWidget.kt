package me.bluegecko.framelessviewer

import java.awt.Dimension
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.TransferHandler

class ImageWidget(val data: ImageWidgetData) : JLabel() {
    lateinit var fileList: MutableList<String>
    private val extensionRegex: Regex = Regex(".jpg|.jpeg|.png|.gif|.bmp|.dib|.wbmp|.webp", RegexOption.IGNORE_CASE)

    init {
        this.horizontalAlignment = CENTER
        this.verticalAlignment = CENTER

        minimumSize = Dimension()

        transferHandler = DropFileHandler()
    }

    inner class DropFileHandler : TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDrop) {
                return false
            }
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false
            }
            return true
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) {
                return false
            }
            val t = support.transferable
            try {
                val files = t.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                val filePath = files[0].toString()
                if (!filePath.contains(extensionRegex)) {
                    return false
                }
                data.imagePath = filePath
                updateImage()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return true
        }
    }

    fun updateImage() {
        try {
            // add support for webp
            ImageIO.scanForPlugins()
            ImageIO.getImageReadersByFormatName("webp").next()

            if (data.imagePath.isEmpty()) {
                return
            }

            val file = File(data.imagePath)
            val image = ImageIO.read(file)

            if (image.width > width || image.height > height) {
                val widthStandardSize = Pair(
                    width, scaledSize(image.width, image.height, width)
                )
                val heightStandardSize = Pair(
                    scaledSize(image.height, image.width, height), height
                )

                if (width >= widthStandardSize.first && height >= widthStandardSize.second) {
                    val scaledImage = image.getScaledInstance(
                        widthStandardSize.first,
                        widthStandardSize.second,
                        Image.SCALE_SMOOTH
                    )
                    this.icon = ImageIcon(scaledImage)
                } else if (width >= heightStandardSize.first && height >= heightStandardSize.second) {
                    val scaledImage = image.getScaledInstance(
                        heightStandardSize.first,
                        heightStandardSize.second,
                        Image.SCALE_SMOOTH
                    )
                    this.icon = ImageIcon(scaledImage)
                }
            } else {
                this.icon = ImageIcon(image)
            }

            updateFileList()

            repaint()
            revalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateFileList() {
        val parentDir = Paths.get(data.imagePath).parent.toString()
        val dir = File(parentDir)
        val fileList =
            dir.listFiles()?.filter { it.isFile }?.map { it.absolutePath.toString() }
                ?.filter { it.contains(extensionRegex) }
        fileList?.let { Collections.sort(it, String.CASE_INSENSITIVE_ORDER) }
        this.fileList = fileList as MutableList<String>
    }

    fun updateTitle() {
        try {
            data.parent.title =
                "${File(data.imagePath).name} [${fileList.indexOf(data.imagePath) + 1}/${fileList.size}] | FramelessViewer"
        } catch (_: Exception) {
        }
    }

    // size1: 1920, size2: 1080, standardSize: 1600 => 900
    private fun scaledSize(size1: Int, size2: Int, standardSize: Int): Int {
        var x = size1
        var y = size2
        while (y != 0) {
            val tmp = y
            y = x % y
            x = tmp
        }

        val aspectRatio = size1 / x to size2 / x

        return (standardSize * aspectRatio.second) / aspectRatio.first
    }
}