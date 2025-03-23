package me.bluegecko.framelessviewer

import kotlinx.coroutines.flow.MutableStateFlow
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
    val channel: AtomicReference<Channel>,
    val uuid: String,
    val appData: MutableStateFlow<AppData>
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
        isUndecorated = appData.value.isUndecorated
        bounds = appData.value.bounds
        layout = null
        isVisible = true

        updateAppSize()

        addPropertyChangeListener("bounds") { event ->
            if (event.newValue != appData.value.bounds) {
                val b = event.newValue as Rectangle
                val newWidth: Int
                val newHeight: Int
                if (!isUndecorated) {
                    newWidth = b.width - insets.left - insets.right
                    newHeight = b.height - insets.top - insets.bottom
                } else {
                    newWidth = b.width
                    newHeight = b.height
                }
                appData.value.bounds = Rectangle(b.x, b.y, newWidth, newHeight)
            }
        }

        addPropertyChangeListener("isUndecorated") { event ->
            if (event.newValue != appData.value.isUndecorated) {
                appData.value.isUndecorated = event.newValue as Boolean
            }
        }

        if (appData.value.panelDataList.isNotEmpty()) {
            appData.value.panelDataList.forEach {
                val panel = createNewPanel(it.imagePath)
                panel.bounds = it.bounds
            }

            repaint()
            revalidate()
        }

        if (appData.value.initPath.isNotEmpty()) {
            createNewPanel(appData.value.initPath)
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

            if (appData.value.isLocked) {
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

            if (appData.value.isLocked) {
                focusedPanel.bounds = Rectangle(0, 0, appWidth, appHeight)
                repaint()
                revalidate()
            }

            getPanels().forEach { it.updateImageSize() }
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
        val runnableMap: Map<String, Runnable> = mapOf(
            "runnableLeftCtrl" to Runnable {
                if (appData.value.isLocked) return@Runnable
                focusedPanel.bounds =
                    Rectangle(0, focusedPanel.y, appWidth / panelDivisor, focusedPanel.height)
                focusedPanel.updateImageSize()
            },
            "runnableRightCtrl" to Runnable {
                if (appData.value.isLocked) return@Runnable
                focusedPanel.bounds =
                    Rectangle(
                        appWidth - appWidth / panelDivisor,
                        focusedPanel.y,
                        appWidth / panelDivisor,
                        focusedPanel.height
                    )
                focusedPanel.updateImageSize()
            },
            "runnableUpCtrl" to Runnable {
                if (appData.value.isLocked) return@Runnable
                focusedPanel.bounds =
                    Rectangle(focusedPanel.x, 0, focusedPanel.width, appHeight / panelDivisor)
                focusedPanel.updateImageSize()
            },
            "runnableDownCtrl" to Runnable {
                if (appData.value.isLocked) return@Runnable
                focusedPanel.bounds =
                    Rectangle(
                        focusedPanel.x,
                        appHeight - appHeight / panelDivisor,
                        focusedPanel.width,
                        appHeight / panelDivisor
                    )
                focusedPanel.updateImageSize()
            },
            "runnableLeftAlt" to Runnable {
                if (appData.value.isLocked) return@Runnable
                focusedPanel.bounds =
                    Rectangle(
                        focusedPanel.x,
                        focusedPanel.y,
                        focusedPanel.width / panelDivisor,
                        focusedPanel.height
                    )
                focusedPanel.updateImageSize()
            },
            "runnableRightAlt" to Runnable {
                if (appData.value.isLocked) return@Runnable
                focusedPanel.bounds =
                    Rectangle(
                        focusedPanel.x + focusedPanel.width / panelDivisor,
                        focusedPanel.y,
                        focusedPanel.width / panelDivisor,
                        focusedPanel.height
                    )
                focusedPanel.updateImageSize()
            },
            "runnableUpAlt" to Runnable {
                if (appData.value.isLocked) return@Runnable
                focusedPanel.bounds =
                    Rectangle(
                        focusedPanel.x,
                        focusedPanel.y,
                        focusedPanel.width,
                        focusedPanel.height / panelDivisor
                    )
                focusedPanel.updateImageSize()
            },
            "runnableDownAlt" to Runnable {
                if (appData.value.isLocked) return@Runnable
                focusedPanel.bounds =
                    Rectangle(
                        focusedPanel.x,
                        focusedPanel.y + focusedPanel.height / panelDivisor,
                        focusedPanel.width,
                        focusedPanel.height / panelDivisor
                    )
                focusedPanel.updateImageSize()
            },
            "runnableUp" to Runnable {
                focusedPanel.bounds = Rectangle(0, 0, appWidth, appHeight)
                repaint()
                revalidate()
                focusedPanel.updateImageSize()
            },
            "runnableDown" to Runnable {
                panelDivisor = 5 - panelDivisor
                updateTitle()
            },
            "runnableLeft" to Runnable {
                if (focusedPanel.imagePath.isEmpty()) return@Runnable
                val fileList = focusedPanel.fileList.toList()
                val fileListIndex = fileList.indexOf(focusedPanel.imagePath)
                if (fileListIndex - 1 < 0) {
                    focusedPanel.imagePath = fileList[fileList.size - 1]
                } else {
                    focusedPanel.imagePath = fileList[fileListIndex - 1]
                }
                focusedPanel.updateImage()
            },
            "runnableRight" to Runnable {
                if (focusedPanel.imagePath.isEmpty()) return@Runnable
                val fileList = focusedPanel.fileList.toList()
                val fileListIndex = fileList.indexOf(focusedPanel.imagePath)
                if (fileListIndex + 1 >= fileList.size) {
                    focusedPanel.imagePath = fileList[0]
                } else {
                    focusedPanel.imagePath = fileList[fileListIndex + 1]
                }
                focusedPanel.updateImage()
            },
            "runnablePageUp" to Runnable {
                if (appData.value.isLocked) return@Runnable

                val panels = getPanels()
                val index = panels.indexOf(focusedPanel)

                if (index + 1 >= panels.size) focusToPanel(panels[0])
                else focusToPanel(panels[index + 1])
            },
            "runnablePageDown" to Runnable {
                if (appData.value.isLocked) return@Runnable

                val panels = getPanels()
                val index = panels.indexOf(focusedPanel)

                if (index - 1 < 0) focusToPanel(panels[panels.size - 1])
                else focusToPanel(panels[index - 1])
            }
        )

        init {
            runnableMap["runnableLeftCtrl"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_LEFT, ctrl = true)] = it
            }
            runnableMap["runnableRightCtrl"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_RIGHT, ctrl = true)] = it
            }
            runnableMap["runnableUpCtrl"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_UP, ctrl = true)] = it
            }
            runnableMap["runnableDownCtrl"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_DOWN, ctrl = true)] = it
            }
            runnableMap["runnableLeftAlt"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_LEFT, alt = true)] = it
            }
            runnableMap["runnableRightAlt"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_RIGHT, alt = true)] = it
            }
            runnableMap["runnableUpAlt"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_UP, alt = true)] = it
            }
            runnableMap["runnableDownAlt"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_DOWN, alt = true)] = it
            }
            runnableMap["runnableUp"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_UP)] = it
            }
            runnableMap["runnableDown"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_DOWN)] = it
            }
            runnableMap["runnableLeft"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_LEFT)] = it
            }
            runnableMap["runnableRight"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_RIGHT)] = it
            }
            runnableMap["runnablePageUp"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_PAGE_UP)] = it
            }
            runnableMap["runnablePageDown"]?.let {
                keybindingMap[KeyData(KeyEvent.VK_PAGE_DOWN)] = it
            }

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
            if (appData.value.isLocked) return

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
                if (appData.value.isLocked) {
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
        appData.value.bounds = Rectangle(this.x, this.y, this.width, this.height)
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
            title =
                "${appController.getShortUUID(uuid)} | PD:${panelDivisor}"
        }
    }

    fun updateAppData() {
        //appData.value.isLocked
        appData.value.panelDataList = convertToPanelData()
        //appData.value.initPath
        appData.value.bounds = bounds
        appData.value.isUndecorated = isUndecorated
    }
}