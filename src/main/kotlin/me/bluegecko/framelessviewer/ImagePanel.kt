package me.bluegecko.framelessviewer

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Paths
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.TransferHandler
import javax.swing.border.LineBorder
import kotlin.math.abs

class ImagePanel(val app: App, data: ImagePanelData) : JPanel() {
    var imagePath = data.imagePath
    lateinit var fileList: Sequence<String>
    lateinit var image: BufferedImage
    lateinit var scaledImage: BufferedImage
    val extensionRegex = Regex(".jpg|.jpeg|.png|.gif|.bmp|.dib|.wbmp|.webp", RegexOption.IGNORE_CASE)
    var zoomRatio = 1.0
    var translateX = 0
    var translateY = 0

    init {
        border = LineBorder(app.defaultColor, 1)
        background = Color.GRAY
        bounds = data.bounds
        layout = GridBagLayout()

        val listener = DraggableListener()
        addMouseListener(listener)
        addMouseMotionListener(listener)
        addMouseWheelListener(ZoomListener())
        transferHandler = DropFileHandler()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

        if (imagePath.isNotEmpty() && ::scaledImage.isInitialized) {
            val x = (width - scaledImage.width) / 2 + translateX
            val y = (height - scaledImage.height) / 2 + translateY

            g2d.drawImage(
                scaledImage,
                x,
                y,
                scaledImage.width,
                scaledImage.height,
                this
            )
        }
    }

    fun updateImageSize() {
        if (imagePath.isEmpty() || !::image.isInitialized) return

        var img: Image? = null

        if (image.width > width || image.height > height) {
            val widthStandardSize = Pair(
                width, scaledSize(image.width, image.height, width)
            )
            val heightStandardSize = Pair(
                scaledSize(image.height, image.width, height), height
            )

            if (width >= widthStandardSize.first && height >= widthStandardSize.second) {
                img = image.getScaledInstance(
                    (widthStandardSize.first * zoomRatio).toInt(),
                    (widthStandardSize.second * zoomRatio).toInt(),
                    Image.SCALE_SMOOTH
                )
            } else if (width >= heightStandardSize.first && height >= heightStandardSize.second) {
                img = image.getScaledInstance(
                    (heightStandardSize.first * zoomRatio).toInt(),
                    (heightStandardSize.second * zoomRatio).toInt(),
                    Image.SCALE_SMOOTH
                )
            }
        } else {
            img =
                image.getScaledInstance(
                    (image.width * zoomRatio).toInt(),
                    (image.height * zoomRatio).toInt(),
                    Image.SCALE_SMOOTH
                )
        }

        if (img != null) {
            scaledImage = toBufferedImage(img)
        }

        repaint()
        revalidate()
    }

    fun updateImage() {
        try {
            if (imagePath.isEmpty()) return

            val file = File(imagePath)
            image = ImageIO.read(file)

            updateImageSize()
            updateFileList()

            repaint()
            revalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            app.updateTitle()
        }
    }

    private fun updateFileList() {
        val dir = Paths.get(imagePath).parent.toFile()
        fileList = dir.walk()
            .filter { it.isFile && it.name.contains(extensionRegex) }
            .map { it.absolutePath }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
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

    private fun toBufferedImage(img: Image): BufferedImage {
        val bufferedImage = BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB)

        val graphics = bufferedImage.createGraphics()
        graphics.drawImage(img, 0, 0, null)
        graphics.dispose()

        return bufferedImage
    }

    inner class DraggableListener : MouseAdapter() {
        private val snapDistance = 20
        private val minimumSize = 50

        private lateinit var initClick: Point

        override fun mousePressed(e: MouseEvent) {
            initClick = e.point

            if (!app.isLocked) app.focusToPanel(this@ImagePanel)

            if (SwingUtilities.isRightMouseButton(e)) {
                app.popupMenu.show(e.component, e.x, e.y)
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                zoomRatio = 1.0
                translateX = 0
                translateY = 0
                updateImageSize()
            } else if (isNearCorner(e.x, e.y)) {
                if (app.isLocked) return

                this@ImagePanel.cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)
            }
        }

        override fun mouseDragged(e: MouseEvent) {
            if (this@ImagePanel.cursor.type == Cursor.SE_RESIZE_CURSOR && !app.isLocked) {
                val newWidth = snapToEdge(e.x, this@ImagePanel.parent.width - this@ImagePanel.x)
                val newHeight = snapToEdge(e.y, this@ImagePanel.parent.height - this@ImagePanel.y)
                this@ImagePanel.size = Dimension(maxOf(newWidth, minimumSize), maxOf(newHeight, minimumSize))
            } else if (zoomRatio > 1.0) {
                val dx = e.x - initClick.x
                val dy = e.y - initClick.y
                translateX += dx
                translateY += dy
                initClick = e.point
                this@ImagePanel.repaint()
                this@ImagePanel.revalidate()
                return
            } else if (!app.isLocked) {
                var newX = snapToEdge(
                    this@ImagePanel.x + e.x - initClick.x,
                    this@ImagePanel.parent.width - this@ImagePanel.width
                )
                var newY = snapToEdge(
                    this@ImagePanel.y + e.y - initClick.y,
                    this@ImagePanel.parent.height - this@ImagePanel.height
                )

                val sto = snapToOther(
                    newX, newY,
                    this@ImagePanel.width,
                    this@ImagePanel.height
                )

                if (sto != null) {
                    newX = sto.first
                    newY = sto.second
                }

                this@ImagePanel.location = Point(newX, newY)
            }
            this@ImagePanel.repaint()
            this@ImagePanel.revalidate()
        }

        override fun mouseReleased(e: MouseEvent) {
            this@ImagePanel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
            this@ImagePanel.updateImageSize()
        }

        private fun snapToEdge(position: Int, max: Int): Int {
            if (app.isPressedShiftKey) return position
            if (abs(position) < snapDistance) return 0
            if (abs(position - max) < snapDistance) return max
            return position
        }

        private fun snapToOther(x: Int, y: Int, width: Int, height: Int): Pair<Int, Int>? {
            if (app.isPressedShiftKey) return null

            for (component in app.getPanels()) {
                if (component === this@ImagePanel) continue

                val other = component.bounds
                var newX = x
                var newY = y

                if ((other.y <= y && y <= other.height + other.y) || (other.y <= y + height && y + height <= other.height + other.y) || (y < other.y && other.y + other.height < y + height)) {
                    if (abs(x - other.x + width) < snapDistance) newX = other.x - width
                    if (abs(x - (other.x + other.width)) < snapDistance) newX = other.x + other.width
                }
                if ((other.x <= x && x <= other.width + other.x) || (other.x <= x + width && x + width <= other.width + other.x) || (x <= other.x && other.x + other.width < x + width)) {
                    if (abs(y - other.y + height) < snapDistance) newY = other.y - height
                    if (abs(y - (other.y + other.height)) < snapDistance) newY = other.y + other.height
                }

                if (newX != x || newY != y) return newX to newY
            }
            return null
        }

        private fun isNearCorner(x: Int, y: Int): Boolean {
            return (x in 0 until snapDistance || x in this@ImagePanel.width - snapDistance until this@ImagePanel.width) &&
                    (y in 0 until snapDistance || y in this@ImagePanel.height - snapDistance until this@ImagePanel.height)
        }
    }

    inner class ZoomListener : MouseWheelListener {
        override fun mouseWheelMoved(e: MouseWheelEvent) {
            when {
                e.preciseWheelRotation < 0 -> zoomRatio += 0.1
                else -> zoomRatio -= 0.1
            }
            if (zoomRatio <= 1.0) {
                translateX = 0
                translateY = 0
            }
            updateImageSize()
        }
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
                imagePath = filePath
                updateImage()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return true
        }
    }
}
