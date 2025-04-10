package me.bluegecko.framelessviewer

import me.bluegecko.framelessviewer.data.AppData
import me.bluegecko.framelessviewer.data.Channel
import me.bluegecko.framelessviewer.data.ChannelMessage
import me.bluegecko.framelessviewer.data.ImagePanelData
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Rectangle
import java.awt.event.*
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder

class App(
    val channel: AtomicReference<Channel>, val uuid: String, val appData: AppData
) : JFrame() {
    val popupMenu = PopupMenu(this)
    var focusedPanel: ImagePanel
    var innerSize = this.contentPane.size
    var isPressedShiftKey = false
    val defaultColor: Color = Color.WHITE
    val focusedColor: Color = Color.CYAN
    var panelDivisor = 2
    val appKeymapsClass: AppKeymaps
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.debug("Initializing App. UUID: $uuid")
        logger.debug(appData.get().toString())

        defaultCloseOperation = DISPOSE_ON_CLOSE
        isUndecorated = appData.get().isUndecorated
        layout = null
        bounds = appData.get().bounds
        isVisible = true

        if (appData.get().panelDataList.isNotEmpty()) {
            appData.get().panelDataList.forEach {
                val panel = createNewPanel(it.imagePath)
                panel.bounds = it.bounds
            }

            repaint()
            revalidate()
        }

        if (appData.get().initPath.isNotEmpty()) {
            createNewPanel(appData.get().initPath)
        }

        if (getPanels().isEmpty()) {
            createNewPanel()
        }

        focusedPanel = getPanels()[0]

        addComponentListener(AppComponentAdapter())
        addWindowListener(AppWindowAdapter())
        appKeymapsClass = AppKeymaps(this)
        addKeyListener(appKeymapsClass)

        SwingUtilities.invokeLater {
            updateAppSize()

            if (appData.get().isLocked) {
                focusedPanel.border = EmptyBorder(0, 0, 0, 0)
                focusedPanel.bounds = Rectangle(0, 0, innerSize.width, innerSize.height)
            } else focusToPanel(getPanels()[0])

            getPanels().forEach { it.updateImage() }

            repaint()
            revalidate()

            val timer = Timer(1000, ImageReceiver())
            timer.start()
        }
    }

    inner class AppComponentAdapter : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            updateAppSize()

            if (appData.get().isLocked) {
                focusedPanel.bounds = Rectangle(0, 0, innerSize.width, innerSize.height)
                repaint()
                revalidate()
            }

            getPanels().forEach { it.updateImageSize() }
        }

        override fun componentMoved(e: ComponentEvent?) {
            updateAppSize()
        }
    }

    inner class AppWindowAdapter : WindowAdapter() {
        override fun windowClosing(e: WindowEvent?) {
            if (e != null) {
                channel.set(Channel(ChannelMessage.Exit))
                this@App.dispose()
            }
        }
    }

    inner class ImageReceiver : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (channel.get().isReceived) {
                val receivedImagePath = channel.get().receivedImagePath
                if (appData.get().isLocked) {
                    focusedPanel.imagePath = receivedImagePath
                    focusedPanel.updateImage()
                } else {
                    createNewPanel(receivedImagePath)
                }

                channel.get().isReceived = false
                channel.get().receivedImagePath = ""
            }
        }
    }

    private fun updateAppSize() {
        innerSize = this.contentPane.size
        appData.applyData { bounds = Rectangle(this@App.x, this@App.y, this@App.width, this@App.height) }
    }

    fun createNewPanel(path: String = ""): ImagePanel {
        val panel = ImagePanel(this, ImagePanelData(Rectangle(innerSize.width, innerSize.height), path))

        this.add(panel)
        panel.repaint()
        panel.revalidate()

        return panel
    }

    fun getPanels(): List<ImagePanel> {
        return this.contentPane.components.filterIsInstance<ImagePanel>().sortedBy { it.uuid }
    }

    private fun convertToPanelData(): MutableList<ImagePanelData> {
        return getPanels().map { ImagePanelData(it.bounds, it.imagePath) }.toMutableList()
    }

    fun focusToPanel(targetPanel: ImagePanel) {
        getPanels().forEach {
            it.border = LineBorder(defaultColor, 1)
        }
        focusedPanel = targetPanel
        targetPanel.border = LineBorder(focusedColor, 1)
        this.contentPane.setComponentZOrder(targetPanel, 0)
        updateTitle()
        repaint()
        revalidate()
    }

    fun updateTitle() {
        try {
            val imageName = File(focusedPanel.imagePath).name
            val nameStr = if (imageName.length < 24) imageName else "${imageName.substring(0, 24)}.."

            title =
                "$nameStr [${focusedPanel.fileList.indexOf(focusedPanel.imagePath) + 1}/${focusedPanel.fileList.toList().size}] | ${
                    appController.getShortUUID(uuid)
                } | PD:${panelDivisor}"
        } catch (e: Exception) {
            title = "${appController.getShortUUID(uuid)} | PD:${panelDivisor}"
        }
    }

    fun updateAppData() {
        updateAppSize()
        appData.applyData {
            panelDataList = convertToPanelData()
            isUndecorated = this@App.isUndecorated
        }
    }
}
