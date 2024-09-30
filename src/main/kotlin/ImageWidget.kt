package me.bluegecko

import java.awt.Component
import java.awt.Image
import java.io.File
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel

class ImageWidget(val data: ImageWidgetData): JLabel() {

    init {
        this.horizontalAlignment = JLabel.CENTER
        this.verticalAlignment = JLabel.CENTER
    }

    fun updateImage() {
        try {
            // add support for webp
            ImageIO.scanForPlugins()
            ImageIO.getImageReadersByFormatName("webp").next()

            val file = File(data.imagePath)
            val image = ImageIO.read(file)

            if (image.width > width || image.height > height) {
                val widthStandardSize = Pair(
                    width, scaledSize(width, height, width)
                )
                val heightStandardSize = Pair(
                    scaledSize(height, width, height), height
                )

                if (width >= widthStandardSize.first && height >= widthStandardSize.second) {
                    val scaledImage = image.getScaledInstance(
                        widthStandardSize.first,
                        widthStandardSize.second,
                        Image.SCALE_SMOOTH
                    )
                    this.icon = ImageIcon(image)
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
        } catch (e: Exception) {
            println(e)
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