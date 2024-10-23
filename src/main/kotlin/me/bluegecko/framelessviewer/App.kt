package me.bluegecko.framelessviewer

import java.awt.*
import java.awt.event.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.abs

class App(private val channel: AtomicReference<Channel>) : JFrame() {
    private var appData = channel.get().initAppData
    var panels = arrayListOf<JPanel>()
    val popupMenu = PopupMenu(this)
    private var focusedPanel: JPanel
    private var appWidth = this.width
    private var appHeight = this.height
    private var isPressedShiftKey = false
    private var isLocked = true
    private val defaultColor = Color.WHITE
    private val focusedColor = Color.CYAN
    private val lockedColor = Color.GRAY

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
        popupMenu.add(itemOpen)
        popupMenu.add(itemClone)
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

        if (panels.isEmpty()) {
            if (appData.imageDataList.isEmpty()) {
                createNewPanel()
            } else {
                appData.imageDataList.forEach { createNewPanel(it.imagePath) }
            }
        }

        focusedPanel = panels[0]
        focusToPanel(panels[0])

        addComponentListener(AppComponentAdapter())
        addWindowListener(AppWindowAdapter())
        addKeyListener(AppKeyAdapter())

        SwingUtilities.invokeLater {
            updateAppSize()

            focusedPanel.size = Dimension(appWidth, appHeight)
            focusedPanel.border = LineBorder(lockedColor, 1)
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

            panels.forEach { getWidget(it).updateImage() }
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

    inner class DraggableListener(panel: JPanel) : MouseAdapter() {
        private val snapDistance = 20
        private val minimumSize = 50

        private var targetPanel: JPanel = panel
        private lateinit var initClick: Point

        override fun mousePressed(e: MouseEvent) {
            initClick = e.point

            if (!isLocked) focusToPanel(targetPanel)

            if (SwingUtilities.isRightMouseButton(e)) {
                popupMenu.show(e.component, e.x, e.y)
            } else if (isNearCorner(e.x, e.y)) {
                if (isLocked) return

                targetPanel.cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)
            }
        }

        override fun mouseDragged(e: MouseEvent) {
            if (isLocked) return

            if (targetPanel.cursor.type == Cursor.SE_RESIZE_CURSOR) {
                val newWidth = snapToEdge(e.x, targetPanel.parent.width - targetPanel.x)
                val newHeight = snapToEdge(e.y, targetPanel.parent.height - targetPanel.y)
                targetPanel.size = Dimension(maxOf(newWidth, minimumSize), maxOf(newHeight, minimumSize))
            } else {
                val newX = snapToEdge(targetPanel.x + e.x - initClick.x, targetPanel.parent.width - targetPanel.width)
                val newY = snapToEdge(targetPanel.y + e.y - initClick.y, targetPanel.parent.height - targetPanel.height)
                targetPanel.location = Point(newX, newY)
            }
            targetPanel.repaint()
            targetPanel.revalidate()
        }

        override fun mouseReleased(e: MouseEvent) {
            targetPanel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        }

        private fun snapToEdge(position: Int, max: Int): Int {
            if (abs(position) < snapDistance) return 0
            if (abs(position - max) < snapDistance) return max

            val parent = targetPanel.parent
            for (component in parent.components) {
                if (component === targetPanel) continue

                val other = component.bounds
                if (abs(position - other.x) < snapDistance) return other.x
                if (abs(position - (other.x + other.width)) < snapDistance) return other.x + other.width
                if (abs(position - other.y) < snapDistance) return other.y
                if (abs(position - (other.y + other.height)) < snapDistance) return other.y + other.height
            }

            return position
        }

        private fun isNearCorner(x: Int, y: Int): Boolean {
            return (x in 0 until snapDistance || x in targetPanel.width - snapDistance until targetPanel.width) &&
                    (y in 0 until snapDistance || y in targetPanel.height - snapDistance until targetPanel.height)
        }
    }

    private fun createDraggablePanel(): JPanel {
        val panel = JPanel()

        panel.border = LineBorder(defaultColor, 1)
        panel.background = Color.GRAY
        panel.bounds = Rectangle(600, 400)

        val listener = DraggableListener(panel)
        panel.addMouseListener(listener)
        panel.addMouseMotionListener(listener)

        return panel
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
        panels.add(panel)
        this.add(panel)

        this.repaint()
        this.revalidate()

        return panel
    }

    private fun getWidget(panel: JPanel): ImageWidget {
        val widget = panel.components.filter { it.javaClass == ImageWidget::class.java }[0] as ImageWidget
        return widget
    }

    private fun convertToPanelData(): MutableList<PanelData> {
        val panelDataList = mutableListOf<PanelData>()
        panels.forEach {
            panelDataList.add(PanelData(it.bounds, getWidget(it).data.imagePath))
        }
        return panelDataList
    }

    private fun focusToPanel(targetPanel: JPanel) {
        panels.forEach { it.border = LineBorder(defaultColor, 1) }
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
            panels.forEach { getWidget(it).updateImage() }
        }
    }

    private fun itemCloneFun() {
        channel.set(Channel(ChannelMessage.NewWindowWithImage, AppData(panelDataList = convertToPanelData())))
    }

    private fun itemLockFun() {
        isLocked = !isLocked
        focusedPanel.border = if (isLocked) LineBorder(lockedColor, 1) else LineBorder(focusedColor, 1)
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
        panels.remove(focusedPanel)

        if (panels.isEmpty()) {
            createNewPanel()
        }

        focusToPanel(panels[0])
    }

    private fun itemExitFun() {
        channel.set(Channel(ChannelMessage.Exit, AppData()))
        this.dispose()
    }
}