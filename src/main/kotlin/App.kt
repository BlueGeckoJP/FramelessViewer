package me.bluegecko

import PanelData
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
        val itemToggleTitle = JMenuItem("Toggle Title")
        val itemRemoveWidget = JMenuItem("Remove Widget")
        val itemExit = JMenuItem("Exit")
        itemNew.addActionListener { itemNewFun() }
        itemNewWidget.addActionListener { itemNewWidgetFun() }
        itemOpen.addActionListener { itemOpenFun() }
        itemClone.addActionListener { itemCloneFun() }
        itemToggleTitle.addActionListener { itemToggleTitleFun() }
        itemRemoveWidget.addActionListener { itemRemoveWidgetFun() }
        itemExit.addActionListener { itemExitFun() }

        popupMenu.add(itemNew)
        popupMenu.add(itemNewWidget)
        popupMenu.add(itemOpen)
        popupMenu.add(itemClone)
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

        addComponentListener(AppComponentListener())
        addWindowListener(AppWindowListener())
        addKeyListener(AppKeyListener())

        SwingUtilities.invokeLater {
            updateAppSize()

            focusedPanel.size = Dimension(appWidth, appHeight)
            val widget = getWidget(focusedPanel)
            widget.updateImage()

            repaint()
            revalidate()
        }
    }

    inner class AppComponentListener : ComponentListener {
        override fun componentMoved(e: ComponentEvent?) {}
        override fun componentShown(e: ComponentEvent?) {}
        override fun componentHidden(e: ComponentEvent?) {}

        override fun componentResized(e: ComponentEvent?) {
            updateAppSize()
            panels.forEach { getWidget(it).updateImage() }
        }
    }

    inner class AppWindowListener : WindowListener {
        override fun windowOpened(e: WindowEvent?) {}
        override fun windowClosed(e: WindowEvent?) {}
        override fun windowIconified(e: WindowEvent?) {}
        override fun windowDeiconified(e: WindowEvent?) {}
        override fun windowActivated(e: WindowEvent?) {}
        override fun windowDeactivated(e: WindowEvent?) {}

        override fun windowClosing(e: WindowEvent?) {
            if (e != null) {
                channel.set(Channel(ChannelMessage.Exit, AppData()))
                this@App.dispose()
            }
        }
    }

    inner class AppKeyListener : KeyListener {
        override fun keyTyped(e: KeyEvent?) {}
        override fun keyPressed(e: KeyEvent?) {}

        override fun keyReleased(e: KeyEvent?) {
            if (e != null) {
                val widget = getWidget(focusedPanel)

                if (widget.data.imagePath.isNotEmpty()) {
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

        private var targetPanel: JPanel = panel
        private lateinit var initClick: Point

        override fun mousePressed(e: MouseEvent?) {
            if (e != null) {
                initClick = e.point
                val widget = getWidget(targetPanel)
                focusToPanel(targetPanel)
                widget.updateTitle()

                if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(e.component, e.x, e.y)
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    val isNearEdge =
                        { coord: Int, size: Int -> coord in (0 until snapDistance) || coord in (size - snapDistance until size) }
                    val isNearCorner =
                        { x: Int, y: Int -> isNearEdge(x, targetPanel.width) && isNearEdge(y, targetPanel.height) }

                    if (isNearCorner(e.x, e.y) == (e.x > snapDistance && e.y > snapDistance)) {
                        targetPanel.cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)
                    }
                }
            }
        }

        override fun mouseDragged(e: MouseEvent?) {
            if (e != null) {
                if (targetPanel.cursor.type == Cursor.SE_RESIZE_CURSOR) {
                    val newWidth = snap(e.x, appWidth)
                    val newHeight = snap(e.y, appHeight)

                    val snapped = snapToPanel(newWidth, newHeight)

                    targetPanel.size = Dimension(snapped.first, snapped.second)
                } else {
                    val panelX = targetPanel.x
                    val panelY = targetPanel.y
                    val mouseX = e.x + panelX
                    val mouseY = e.y + panelY

                    var newX = panelX + (mouseX - initClick.x - panelX)
                    var newY = panelY + (mouseY - initClick.y - panelY)

                    newX = snap(newX, appWidth - targetPanel.width)
                    newY = snap(newY, appHeight - targetPanel.height)

                    targetPanel.location = Point(newX, newY)
                }
            }
        }

        override fun mouseReleased(e: MouseEvent?) {
            if (e != null) {
                if (targetPanel.cursor.type == Cursor.SE_RESIZE_CURSOR) {
                    targetPanel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    val widget = getWidget(targetPanel)
                    widget.size = Dimension(e.x, e.y)
                    widget.updateImage()
                }
            }
        }

        private fun snap(position: Int, max: Int): Int {
            if (abs(position) < snapDistance) return 0
            if (abs(position - max) < snapDistance) return max
            return position
        }

        private fun snapToPanel(x: Int, y: Int): Pair<Int, Int> {
            panels.forEach {
                if (abs(x - it.x) <= snapDistance && abs(y - it.y) <= snapDistance) return it.x to it.y
                if (abs(x - it.x + it.width) <= snapDistance && abs(y - it.y + it.height) <= snapDistance) return it.x + it.width to it.y + it.height
                if (abs(x - it.x) <= snapDistance) return it.x to y
                if (abs(x - it.x + it.width) <= snapDistance) return it.x + it.width to y
                if (abs(y - it.y) <= snapDistance) return x to it.y
                if (abs(y - it.y + it.height) <= snapDistance) return x to it.y + it.height
            }
            return x to y
        }
    }

    private fun createDraggablePanel(): JPanel {
        val panel = JPanel()

        panel.border = LineBorder(Color.GRAY, 1)
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

        println("$appWidth, $appHeight        $width, $height")
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
        panels.forEach { it.border = LineBorder(Color.GRAY, 1) }
        focusedPanel = targetPanel
        targetPanel.border = LineBorder(Color.CYAN, 1)
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