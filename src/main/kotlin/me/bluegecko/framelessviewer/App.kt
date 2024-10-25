package me.bluegecko.framelessviewer

import java.awt.*
import java.awt.event.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.filechooser.FileNameExtensionFilter

class App(private val channel: AtomicReference<Channel>) : JFrame() {
    private var appData = channel.get().initAppData
    val popupMenu = PopupMenu(this)
    private var focusedPanel: JPanel
    private var appWidth = this.width
    private var appHeight = this.height
    private var isPressedShiftKey = false
    var isLocked = true
    val defaultColor: Color = Color.WHITE
    private val focusedColor: Color = Color.CYAN

    init {
        title = "FramelessViewer"
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
        val itemRemoveWidget = JMenuItem("Remove Widget")
        val itemExit = JMenuItem("Exit")
        itemNew.addActionListener { itemNewFun() }
        itemNewWidget.addActionListener { itemNewWidgetFun() }
        itemOpen.addActionListener { itemOpenFun() }
        itemClone.addActionListener { itemCloneFun() }
        itemLock.addActionListener { itemLockFun() }
        itemToggleTitle.addActionListener { itemToggleTitleFun() }
        itemRemoveWidget.addActionListener { itemRemoveWidgetFun() }
        itemExit.addActionListener { itemExitFun() }
        popupMenu.add(itemNew)
        popupMenu.add(itemNewWidget)
        popupMenu.addSeparator()
        popupMenu.add(itemOpen)
        popupMenu.add(itemClone)
        popupMenu.addSeparator()
        popupMenu.add(itemLock)
        popupMenu.add(itemToggleTitle)
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
            if (appData.imageDataList.isEmpty()) {
                createNewPanel()
            } else {
                appData.imageDataList.forEach { createNewPanel(it.imagePath) }
            }
        }

        focusedPanel = getPanels()[0]
        focusToPanel(getPanels()[0])

        addComponentListener(AppComponentAdapter())
        addWindowListener(AppWindowAdapter())
        addKeyListener(AppKeyAdapter())

        SwingUtilities.invokeLater {
            updateAppSize()

            focusedPanel.size = Dimension(appWidth, appHeight)
            focusedPanel.border = EmptyBorder(0, 0, 0, 0)
            val widget = getWidget(focusedPanel)
            widget.updateImage()

            repaint()
            revalidate()
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

            getPanels().forEach { getWidget(it).updateImage() }
        }
    }

    inner class AppWindowAdapter : WindowAdapter() {
        override fun windowClosing(e: WindowEvent?) {
            if (e != null) {
                channel.set(Channel(ChannelMessage.Exit, AppData()))
                this@App.dispose()
            }
        }
    }

    inner class AppKeyAdapter : KeyAdapter() {
        override fun keyPressed(e: KeyEvent?) {
            if (isLocked) return

            if (e != null) {
                if (e.modifiersEx and KeyEvent.SHIFT_DOWN_MASK != 0) isPressedShiftKey = true
            }
        }

        override fun keyReleased(e: KeyEvent?) {
            if (e != null) {
                if (isPressedShiftKey) isPressedShiftKey = false

                val widget = getWidget(focusedPanel)

                if (e.modifiersEx and KeyEvent.CTRL_DOWN_MASK != 0) {
                    if (isLocked) return

                    if (e.keyCode == KeyEvent.VK_LEFT) focusedPanel.bounds =
                        Rectangle(0, focusedPanel.y, appWidth / 2, focusedPanel.height)
                    if (e.keyCode == KeyEvent.VK_RIGHT) focusedPanel.bounds =
                        Rectangle(appWidth / 2, focusedPanel.y, appWidth / 2, focusedPanel.height)
                    if (e.keyCode == KeyEvent.VK_UP) focusedPanel.bounds =
                        Rectangle(focusedPanel.x, 0, focusedPanel.width, appHeight / 2)
                    if (e.keyCode == KeyEvent.VK_DOWN) focusedPanel.bounds =
                        Rectangle(focusedPanel.x, appHeight / 2, focusedPanel.width, appHeight / 2)

                    repaint()
                    revalidate()
                } else if (e.modifiersEx and KeyEvent.ALT_DOWN_MASK != 0) {
                    if (isLocked) return

                    if (e.keyCode == KeyEvent.VK_LEFT) focusedPanel.bounds =
                        Rectangle(focusedPanel.x, focusedPanel.y, focusedPanel.width / 2, focusedPanel.height)
                    if (e.keyCode == KeyEvent.VK_RIGHT) focusedPanel.bounds =
                        Rectangle(
                            focusedPanel.x + focusedPanel.width / 2,
                            focusedPanel.y,
                            focusedPanel.width / 2,
                            focusedPanel.height
                        )
                    if (e.keyCode == KeyEvent.VK_UP) focusedPanel.bounds =
                        Rectangle(focusedPanel.x, focusedPanel.y, focusedPanel.width, focusedPanel.height / 2)
                    if (e.keyCode == KeyEvent.VK_DOWN) focusedPanel.bounds =
                        Rectangle(
                            focusedPanel.x,
                            focusedPanel.y + focusedPanel.height / 2,
                            focusedPanel.width,
                            focusedPanel.height / 2
                        )

                    repaint()
                    revalidate()
                } else if (e.keyCode == KeyEvent.VK_UP) {
                    focusedPanel.bounds = Rectangle(0, 0, appWidth, appHeight)
                    repaint()
                    revalidate()
                } else if (widget.data.imagePath.isNotEmpty()) {
                    val fileListIndex = widget.fileList.indexOf(widget.data.imagePath)

                    if (e.keyCode == KeyEvent.VK_LEFT) {
                        if (fileListIndex - 1 < 0) {
                            widget.data.imagePath = widget.fileList[widget.fileList.size - 1]
                        } else {
                            widget.data.imagePath = widget.fileList[fileListIndex - 1]
                        }
                    } else if (e.keyCode == KeyEvent.VK_RIGHT) {
                        if (fileListIndex + 1 >= widget.fileList.size) {
                            widget.data.imagePath = widget.fileList[0]
                        } else {
                            widget.data.imagePath = widget.fileList[fileListIndex + 1]
                        }
                    }

                    widget.updateImage()
                    widget.updateTitle()
                }
            }
        }
    }

    private fun createDraggablePanel(): JPanel {
        return ImagePanel(this)
    }

    private fun updateAppSize() {
        if (!appData.isUndecorated) {
            appWidth = width - insets.left - insets.right
            appHeight = height - insets.top - insets.bottom
        } else {
            appWidth = width
            appHeight = height
        }
    }

    private fun createNewPanel(path: String = ""): JPanel {
        val panel = createDraggablePanel()
        val widget = ImageWidget(ImageWidgetData(this, path, appWidth, appHeight))

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.gridwidth = 1
        gbc.gridheight = 1

        panel.layout = GridBagLayout()
        panel.add(widget, gbc)
        this.add(panel)

        this.repaint()
        this.revalidate()

        return panel
    }

    private fun getWidget(panel: JPanel): ImageWidget {
        val widget = panel.components.filter { it.javaClass == ImageWidget::class.java }[0] as ImageWidget
        return widget
    }

    private fun getPanels(): List<ImagePanel> {
        return this.contentPane.components.map { it as ImagePanel }
    }

    private fun convertToPanelData(): MutableList<PanelData> {
        val panelDataList = mutableListOf<PanelData>()
        getPanels().forEach {
            panelDataList.add(PanelData(it.bounds, getWidget(it).data.imagePath))
        }
        return panelDataList
    }

    fun focusToPanel(targetPanel: JPanel) {
        getPanels().forEach { it.border = LineBorder(defaultColor, 1) }
        focusedPanel = targetPanel
        targetPanel.border = LineBorder(focusedColor, 1)
    }

    private fun itemNewFun() {
        channel.set(Channel(ChannelMessage.NewWindow, AppData()))
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
            getWidget(focusedPanel).data.imagePath = file.absolutePath
            getPanels().forEach { getWidget(it).updateImage() }
        }
    }

    private fun itemCloneFun() {
        channel.set(Channel(ChannelMessage.NewWindowWithImage, AppData(panelDataList = convertToPanelData())))
    }

    private fun itemLockFun() {
        isLocked = !isLocked
        focusedPanel.border = if (isLocked) EmptyBorder(0, 0, 0, 0) else LineBorder(focusedColor, 1)
        focusedPanel.bounds = Rectangle(0, 0, appWidth, appHeight)

        repaint()
        revalidate()
    }

    private fun itemToggleTitleFun() {
        appData.isUndecorated = !appData.isUndecorated
        appData.bounds = bounds
        appData.panelDataList = convertToPanelData()
        channel.set(Channel(ChannelMessage.Reinit, appData))
        this.dispose()
    }

    private fun itemRemoveWidgetFun() {
        this.remove(focusedPanel)

        if (getPanels().isEmpty()) {
            createNewPanel()
        }

        focusToPanel(getPanels()[0])
    }

    private fun itemExitFun() {
        channel.set(Channel(ChannelMessage.Exit, AppData()))
        this.dispose()
    }
}