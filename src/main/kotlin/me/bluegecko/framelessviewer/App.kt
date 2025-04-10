package me.bluegecko.framelessviewer

import me.bluegecko.framelessviewer.data.*
import org.yaml.snakeyaml.Yaml
import java.awt.Color
import java.awt.Rectangle
import java.awt.event.*
import java.io.File
import java.nio.charset.StandardCharsets
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
    var appWidth = this.width
    var appHeight = this.height
    var isPressedShiftKey = false
    val defaultColor: Color = Color.WHITE
    val focusedColor: Color = Color.CYAN
    var panelDivisor = 2
    val appKeyAdapter: AppKeyAdapter

    init {
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
        appKeyAdapter = AppKeyAdapter()
        addKeyListener(appKeyAdapter)

        SwingUtilities.invokeLater {
            updateAppSize()

            if (appData.get().isLocked) {
                focusedPanel.border = EmptyBorder(0, 0, 0, 0)
                focusedPanel.bounds = Rectangle(0, 0, appWidth, appHeight)
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
                focusedPanel.bounds = Rectangle(0, 0, appWidth, appHeight)
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

    inner class AppKeyAdapter : KeyAdapter() {
        val keybindingMap: MutableMap<KeyData, Runnable> = mutableMapOf()
        val runnableMap: Map<String, Runnable> = mapOf()

        init {
            // Work in progress
            val yaml = Yaml()
            val inputStream = javaClass.getResourceAsStream("/me/bluegecko/framelessviewer/keybinding-override.yml")
                ?: throw IllegalStateException("keybinding-override.yml not found")
            try {
                val keybindingOverrides: Map<String, Any> = inputStream.use { stream ->
                    yaml.load(stream.bufferedReader(StandardCharsets.UTF_8))
                }
                println(keybindingOverrides)

                keybindingOverrides.forEach { keybinding ->
                    val value = keybinding.value as Map<*, *>
                    val keyCode = value["keyCode"] as Int
                    val ctrl = value["ctrl"] as Boolean
                    val shift = value["shift"] as Boolean
                    val alt = value["alt"] as Boolean

                    val runnable = runnableMap[keybinding.key]
                    if (runnable != null) {
                        keybindingMap.remove(keybindingMap.entries.find { it.value == runnable }?.key)
                        keybindingMap[KeyData(keyCode, ctrl, shift, alt)] = runnable
                    }
                }
            } catch (_: Exception) {
            }
        }

        override fun keyPressed(e: KeyEvent?) {
            if (appData.get().isLocked) return

            if (e != null) {
                if (e.modifiersEx and KeyEvent.SHIFT_DOWN_MASK != 0) isPressedShiftKey = true
            }
        }

        override fun keyReleased(e: KeyEvent) {
            if (isPressedShiftKey) isPressedShiftKey = false

            val input = KeyData(e.keyCode, e.isControlDown, e.isShiftDown, e.isAltDown)
            val value = keybindingMap[input]
            value?.run()
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
        if (!isUndecorated) {
            appWidth = width - insets.left - insets.right
            appHeight = height - insets.top - insets.bottom
        } else {
            appWidth = width
            appHeight = height
        }
        appData.applyData { bounds = Rectangle(this@App.x, this@App.y, this@App.width, this@App.height) }
    }

    fun createNewPanel(path: String = ""): ImagePanel {
        val panel = ImagePanel(this, ImagePanelData(Rectangle(appWidth, appHeight), path))

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
