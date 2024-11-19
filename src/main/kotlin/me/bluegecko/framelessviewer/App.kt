package me.bluegecko.framelessviewer

import me.bluegecko.framelessviewer.data.*
import org.yaml.snakeyaml.Yaml
import java.awt.Color
import java.awt.Rectangle
import java.awt.event.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.event.MenuEvent
import javax.swing.event.MenuListener
import javax.swing.filechooser.FileNameExtensionFilter

class App(private val channel: AtomicReference<Channel>, private val uuid: String) : JFrame() {
    private val appData = channel.get().appData
    val popupMenu = PopupMenu(this)
    private var focusedPanel: ImagePanel
    var appWidth = this.width
    var appHeight = this.height
    var isPressedShiftKey = false
    var isLocked = appData.isLocked
    val defaultColor: Color = Color.WHITE
    private val focusedColor: Color = Color.CYAN
    var panelDivisor = 2

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isUndecorated = appData.isUndecorated
        bounds = appData.bounds
        layout = null
        isVisible = true

        updateAppSize()

        val itemNew = JMenuItem("New")
        val itemNewWidget = JMenuItem("New Widget")
        val itemOpen = JMenuItem("Open")
        val itemClone = JMenuItem("Clone")
        val itemLock = JMenuItem("Lock To Window")
        val itemToggleTitle = JMenuItem("Toggle Title")
        val itemFitToImage = JMenuItem("Fit To Image")
        val itemSetZoomRatioToAuto = JMenuItem("Set Zoom Ratio To Auto")
        val itemRemoveWidget = JMenuItem("Remove Widget")
        val itemExit = JMenuItem("Exit")
        itemNew.addActionListener { itemNewFun() }
        itemNewWidget.addActionListener { itemNewWidgetFun() }
        itemOpen.addActionListener { itemOpenFun() }
        itemClone.addActionListener { itemCloneFun() }
        itemLock.addActionListener { itemLockFun() }
        itemToggleTitle.addActionListener { itemToggleTitleFun() }
        itemFitToImage.addActionListener { itemFitToImageFun() }
        itemSetZoomRatioToAuto.addActionListener { itemSetZoomRatioToAutoFun() }
        itemRemoveWidget.addActionListener { itemRemoveWidgetFun() }
        itemExit.addActionListener { itemExitFun() }

        val menuSendImageTo = JMenu("Send Image To")
        menuSendImageTo.addMenuListener(object : MenuListener {
            override fun menuSelected(e: MenuEvent) {
                menuSendImageTo.removeAll()

                getThreadUUIDs().forEach {
                    if (it != uuid) {
                        val otherUUID = it
                        val item = JMenuItem(getShortUUID(otherUUID))
                        item.addActionListener { sendImageTo(otherUUID) }
                        menuSendImageTo.add(item)
                    }
                }
            }

            override fun menuDeselected(e: MenuEvent?) {}
            override fun menuCanceled(e: MenuEvent?) {}
        })

        popupMenu.add(itemNew)
        popupMenu.add(itemNewWidget)
        popupMenu.addSeparator()
        popupMenu.add(itemOpen)
        popupMenu.add(itemClone)
        popupMenu.addSeparator()
        popupMenu.add(itemLock)
        popupMenu.add(itemToggleTitle)
        popupMenu.add(itemFitToImage)
        popupMenu.add(itemSetZoomRatioToAuto)
        popupMenu.add(menuSendImageTo)
        popupMenu.addSeparator()
        popupMenu.add(itemRemoveWidget)
        popupMenu.add(itemExit)

        if (appData.panelDataList.isNotEmpty()) {
            appData.panelDataList.forEach {
                val panel = createNewPanel(it.imagePath)
                panel.bounds = it.bounds
            }

            repaint()
            revalidate()
        }

        if (appData.initPath.isNotEmpty()) {
            createNewPanel(appData.initPath)
        }

        if (getPanels().isEmpty()) {
            createNewPanel()
        }

        focusedPanel = getPanels()[0]

        addComponentListener(AppComponentAdapter())
        addWindowListener(AppWindowAdapter())
        addKeyListener(AppKeyAdapter())

        SwingUtilities.invokeLater {
            updateAppSize()

            if (isLocked) {
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

            if (isLocked) {
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
        private val keyBindMap: MutableMap<KeyData, Runnable> = mutableMapOf()
        private val runnableMap: Map<String, Runnable> = mapOf(
            "runnableLeftCtrl" to Runnable {
                if (isLocked) return@Runnable
                focusedPanel.bounds =
                    Rectangle(0, focusedPanel.y, appWidth / panelDivisor, focusedPanel.height)
                focusedPanel.updateImageSize()
            },
            "runnableRightCtrl" to Runnable {
                if (isLocked) return@Runnable
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
                if (isLocked) return@Runnable
                focusedPanel.bounds =
                    Rectangle(focusedPanel.x, 0, focusedPanel.width, appHeight / panelDivisor)
                focusedPanel.updateImageSize()
            },
            "runnableDownCtrl" to Runnable {
                if (isLocked) return@Runnable
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
                if (isLocked) return@Runnable
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
                if (isLocked) return@Runnable
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
                if (isLocked) return@Runnable
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
                if (isLocked) return@Runnable
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
                updateTitle()
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
                updateTitle()
            }
        )

        init {
            runnableMap["runnableLeftCtrl"]?.let {
                keyBindMap[KeyData(KeyEvent.VK_LEFT, ctrl = true)] = it
            }
            runnableMap["runnableRightCtrl"]?.let {
                keyBindMap[KeyData(KeyEvent.VK_RIGHT, ctrl = true)] = it
            }
            runnableMap["runnableUpCtrl"]?.let {
                keyBindMap[KeyData(KeyEvent.VK_UP, ctrl = true)] = it
            }
            runnableMap["runnableDownCtrl"]?.let {
                keyBindMap[KeyData(KeyEvent.VK_DOWN, ctrl = true)] = it
            }
            runnableMap["runnableLeftAlt"]?.let {
                keyBindMap[KeyData(KeyEvent.VK_LEFT, alt = true)] = it
            }
            runnableMap["runnableRightAlt"]?.let {
                keyBindMap[KeyData(KeyEvent.VK_RIGHT, alt = true)] = it
            }
            keyBindMap[KeyData(KeyEvent.VK_UP, alt = true)] = runnableUpAlt
            keyBindMap[KeyData(KeyEvent.VK_DOWN, alt = true)] = runnableDownAlt
            keyBindMap[KeyData(KeyEvent.VK_UP)] = runnableUp
            keyBindMap[KeyData(KeyEvent.VK_DOWN)] = runnableDown
            keyBindMap[KeyData(KeyEvent.VK_LEFT)] = runnableLeft
            keyBindMap[KeyData(KeyEvent.VK_RIGHT)] = runnableRight

            val yaml = Yaml()
            val inputStream = javaClass.getResourceAsStream("/me/bluegecko/framelessviewer/keybinding-override.yml")
                ?: throw IllegalStateException("keybinding-override.yml not found")
            try {
                val keybindingOverrides: Map<String, KeyData> = inputStream.use { stream ->
                    yaml.load(stream.bufferedReader(StandardCharsets.UTF_8))
                }
                println(keybindingOverrides)
            } catch (_: Exception) {
                println("Keybinding overrides is empty")
            }
        }

        override fun keyPressed(e: KeyEvent?) {
            if (isLocked) return

            if (e != null) {
                if (e.modifiersEx and KeyEvent.SHIFT_DOWN_MASK != 0) isPressedShiftKey = true
            }
        }

        override fun keyReleased(e: KeyEvent) {
            if (isPressedShiftKey) isPressedShiftKey = false

            val input = KeyData(e.keyCode, e.isControlDown, e.isShiftDown, e.isAltDown)
            val value = keyBindMap[input]
            value?.run()
        }
    }

    inner class ImageReceiver : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (channel.get().isReceived) {
                val receivedImagePath = channel.get().receivedImagePath
                if (isLocked) {
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
    }

    private fun createNewPanel(path: String = ""): ImagePanel {
        val panel = ImagePanel(this, ImagePanelData(Rectangle(appWidth, appHeight), path))

        this.add(panel)
        panel.repaint()
        panel.revalidate()

        return panel
    }

    fun getPanels(): List<ImagePanel> {
        return this.contentPane.components.map { it as ImagePanel }
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
    }

    private fun createExportAppData(): AppData {
        return appData.copy(
            isUndecorated = isUndecorated,
            bounds = bounds,
            panelDataList = convertToPanelData(),
            isLocked = isLocked
        )
    }

    private fun sendImageTo(target: String) {
        val uuids = getThreadUUIDs()
        if (uuids.contains(target)) {
            channel.set(
                Channel(
                    message = ChannelMessage.SendImage,
                    sendImageTo = target,
                    sendImagePath = focusedPanel.imagePath
                )
            )
        }
    }

    fun getShortUUID(uuid: String): String {
        return uuid.substringBefore("-")
    }

    fun updateTitle() {
        try {
            val imageName = File(focusedPanel.imagePath).name
            val nameStr = if (imageName.length < 24) imageName else "${imageName.substring(0, 24)}.."

            title =
                "$nameStr [${focusedPanel.fileList.indexOf(focusedPanel.imagePath) + 1}/${focusedPanel.fileList.toList().size}] | ${
                    getShortUUID(uuid)
                } | PD:${panelDivisor}"
        } catch (e: Exception) {
            title =
                "${getShortUUID(uuid)} | PD:${panelDivisor}"
        }
    }

    private fun itemNewFun() {
        channel.set(Channel(ChannelMessage.NewWindow))
    }

    private fun itemNewWidgetFun() {
        createNewPanel()
    }

    private fun itemOpenFun() {
        val chooser = JFileChooser()
        chooser.fileFilter = FileNameExtensionFilter("JPEG", "jpg", "jpeg")
        chooser.fileFilter = FileNameExtensionFilter("PNG", "png")
        chooser.fileFilter = FileNameExtensionFilter("GIF", "gif")
        chooser.fileFilter = FileNameExtensionFilter("BMP", "bmp", "dib")
        chooser.fileFilter = FileNameExtensionFilter("WBMP", "wbmp")
        chooser.fileFilter = FileNameExtensionFilter("WebP", "webp")
        chooser.fileFilter =
            FileNameExtensionFilter("Supported images", "jpg", "jpeg", "png", "gif", "bmp", "dib", "wbmp", "webp")
        chooser.showOpenDialog(null)
        val file = chooser.selectedFile

        if (file != null) {
            focusedPanel.imagePath = file.absolutePath
            focusedPanel.zoomRatio = 1.0
            focusedPanel.updateImage()
        }
    }

    private fun itemCloneFun() {
        channel.set(Channel(ChannelMessage.NewWindowWithImage, createExportAppData()))
    }

    private fun itemLockFun() {
        isLocked = !isLocked
        focusedPanel.border = if (isLocked) EmptyBorder(0, 0, 0, 0) else LineBorder(focusedColor, 1)
        focusedPanel.bounds = Rectangle(0, 0, appWidth, appHeight)

        repaint()
        revalidate()
    }

    private fun itemFitToImageFun() {
        focusedPanel.bounds =
            Rectangle(focusedPanel.x, focusedPanel.y, focusedPanel.scaledImage.width, focusedPanel.scaledImage.height)
        focusedPanel.zoomRatio = 1.0
        focusedPanel.translateX = 0
        focusedPanel.translateY = 0
        focusedPanel.updateImageSize()
    }

    private fun itemSetZoomRatioToAutoFun() {
        focusedPanel.scaledImage = focusedPanel.image
        focusedPanel.repaint()
        focusedPanel.revalidate()
    }

    private fun itemToggleTitleFun() {
        val exportAppData = createExportAppData()
        exportAppData.isUndecorated = !isUndecorated
        channel.set(Channel(ChannelMessage.Reinit, exportAppData))
        this.dispose()
    }

    private fun itemRemoveWidgetFun() {
        this.remove(focusedPanel)

        isLocked = false

        if (getPanels().isEmpty()) {
            createNewPanel()
        }

        focusToPanel(getPanels()[0])

        repaint()
        revalidate()
    }

    private fun itemExitFun() {
        channel.set(Channel(ChannelMessage.Exit))
        this.dispose()
    }
}