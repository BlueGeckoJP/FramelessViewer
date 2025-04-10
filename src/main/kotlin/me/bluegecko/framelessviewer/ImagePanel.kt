package me.bluegecko.framelessviewer

import me.bluegecko.framelessviewer.data.ImagePanelData
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import javax.swing.TransferHandler
import javax.swing.border.LineBorder
import kotlin.math.abs

class ImagePanel(val app: App, data: ImagePanelData) : JPanel() {
    var imagePath = data.imagePath
    lateinit var fileList: Sequence<String>
    lateinit var image: BufferedImage
    val extensionRegex = Regex(".jpg|.jpeg|.png|.gif|.bmp|.dib|.wbmp|.webp", RegexOption.IGNORE_CASE)
    var zoomRatio = 1.0
    var translateX = 0
    var translateY = 0
    var resizedWidth = 0
    var resizedHeight = 0
    private var scaledImage: Image? = null
    val uuid: UUID = UUID.randomUUID()
    private val numRegex = Regex("[0-9]+")

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

        updateImage()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        if (imagePath.isEmpty() || !::image.isInitialized || scaledImage == null) return

        val g2d = g as Graphics2D

        val x = (width - resizedWidth) / 2 + translateX
        val y = (height - resizedHeight) / 2 + translateY

        g2d.drawImage(
            scaledImage,
            x,
            y,
            resizedWidth,
            resizedHeight,
            this
        )
    }

    fun updateImageSize() {
        if (imagePath.isEmpty() || !::image.isInitialized) return

        if (image.width > width || image.height > height) {
            val widthStandardSize = Pair(
                width, scaledSize(image.width, image.height, width)
            )
            val heightStandardSize = Pair(
                scaledSize(image.height, image.width, height), height
            )

            if (width >= widthStandardSize.first && height >= widthStandardSize.second) {
                resizedWidth = (widthStandardSize.first * zoomRatio).toInt()
                resizedHeight = (widthStandardSize.second * zoomRatio).toInt()
            } else if (width >= heightStandardSize.first && height >= heightStandardSize.second) {
                resizedWidth = (heightStandardSize.first * zoomRatio).toInt()
                resizedHeight = (heightStandardSize.second * zoomRatio).toInt()
            }
        } else {
            resizedWidth = (image.width * zoomRatio).toInt()
            resizedHeight = (image.height * zoomRatio).toInt()
        }

        scaledImage = image.getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_SMOOTH)

        repaint()
        revalidate()
    }

    fun updateImage() {
        object : SwingWorker<BufferedImage?, Void>() {
            override fun doInBackground(): BufferedImage? {
                return try {
                    ImageIO.read(File(imagePath))
                } catch (e: Exception) {
                    null
                }
            }

            override fun done() {
                try {
                    if (imagePath.isEmpty()) return

                    val img = get()
                    img?.let {
                        image = it

                        updateImageSize()
                        updateFileList()

                        repaint()
                        revalidate()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    app.updateTitle()
                }
            }
        }.also {
            app.title = "Loading.. ".plus(app.title)
            it.execute()
        }
    }

    private fun updateFileList() {
        val dir = Paths.get(imagePath).parent.toFile()
        dir.listFiles()?.let { files ->
            fileList =
                files.filter { it.isFile && it.name.contains(extensionRegex) }
                    .map { it.absolutePath }
                    .sortedWith(
                        compareBy<String> { numRegex.replace(it, "").lowercase(Locale.getDefault()) }
                            .thenBy { numRegex.findAll(it).map { v -> v.value }.joinToString("").toLongOrNull() ?: 0 }
                    )
                    .asSequence()
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

        return standardSize * aspectRatio.second / aspectRatio.first
    }

    inner class DraggableListener : MouseAdapter() {
        private val snapDistance = 20
        private val minimumSize = 50

        private lateinit var initClick: Point

        override fun mousePressed(e: MouseEvent) {
            initClick = e.point

            if (!app.appData.get().isLocked) app.focusToPanel(this@ImagePanel)

            if (SwingUtilities.isRightMouseButton(e)) {
                app.popupMenu.show(e.component, e.x, e.y)
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                zoomRatio = 1.0
                translateX = 0
                translateY = 0
                updateImageSize()
            } else if (isNearCorner(e.x, e.y)) {
                if (app.appData.get().isLocked) return

                this@ImagePanel.cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)
            }
        }

        override fun mouseDragged(e: MouseEvent) {
            if (this@ImagePanel.cursor.type == Cursor.SE_RESIZE_CURSOR && !app.appData.get().isLocked) {
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
            } else if (!app.appData.get().isLocked) {
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
            zoomRatio *= when {
                e.preciseWheelRotation < 0 -> 1.1
                else -> 0.9
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
