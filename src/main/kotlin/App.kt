package me.bluegecko

import java.awt.*
import java.awt.event.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.abs

class App(private val channel: AtomicReference<Channel>) : JFrame() {
    val snapDistance = 20
    var appData = channel.get().initAppData
    var panels = arrayListOf<JPanel>()
    val popupMenu = PopupMenu(this)
    var focusedPanel: JPanel
    private var appWidth = this.width
    private var appHeight = this.height

    init {
        title = "FramelessViewer"
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isUndecorated = appData.isUndecorated
        bounds = appData.bounds
        layout = null

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
        popupMenu.add(itemRemoveWidget)
        popupMenu.add(itemExit)

        if (appData.initPath.isNotEmpty()) {
            createNewPanel(appData.initPath)
        }

        if (appData.imageDataList.isEmpty()) {
            createNewPanel()
        } else {
            appData.imageDataList.forEach { createNewPanel(it.imagePath) }
        }

        focusedPanel = panels[0]

        addComponentListener(ResizeListener())
        addWindowListener(CloseListener())
        addKeyListener(ArrowKeyListener())
    }

    inner class ResizeListener : ComponentListener {
        override fun componentMoved(e: ComponentEvent?) {}
        override fun componentShown(e: ComponentEvent?) {}
        override fun componentHidden(e: ComponentEvent?) {}

        override fun componentResized(e: ComponentEvent?) {
            updateAppSize()
            panels.forEach { getWidget(it).updateImage() }
        }
    }

    inner class CloseListener : WindowListener {
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

    inner class ArrowKeyListener : KeyListener {
        override fun keyTyped(e: KeyEvent?) {}
        override fun keyPressed(e: KeyEvent?) {}

        override fun keyReleased(e: KeyEvent?) {
            TODO()
        }

    }

    inner class DraggableListener(panel: JPanel) : MouseAdapter() {
        private var targetPanel: JPanel = panel
        private lateinit var initClick: Point

        override fun mousePressed(e: MouseEvent?) {
            if (e != null) {
                initClick = e.point

                val widget = getWidget(targetPanel)

                panels.forEach { it.border = LineBorder(Color.GRAY, 1) }
                targetPanel.border = LineBorder(Color.CYAN, 1)
                widget.updateTitle()
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(e.component, e.x, e.y)
                }
            }
        }

        override fun mouseDragged(e: MouseEvent?) {
            if (e != null) {
                val panelX = targetPanel.x
                val panelY = targetPanel.y
                val mouseX = e.x + panelX
                val mouseY = e.y + panelY

                var newX = panelX + (mouseX - initClick.x - panelX)
                var newY = panelY + (mouseY - initClick.y - panelY)

                newX = snap(newX, width - targetPanel.width)
                newY = snap(newY, height - targetPanel.height)

                targetPanel.location = Point(newX, newY)
            }
        }

        private fun snap(position: Int, max: Int): Int {
            if (abs(position) < snapDistance) return 0
            if (abs(position - max) < snapDistance) return max
            return position
        }
    }

    private fun createDraggablePanel(): JPanel {
        val panel = JPanel()

        panel.border = LineBorder(Color.GRAY, 1)
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
        }
    }

    private fun createNewPanel(path: String = "") {
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
    }

    private fun getWidget(panel: JPanel): ImageWidget {
        return panel.getComponent(0) as ImageWidget
    }

    private fun itemNewFun() {
        channel.set(Channel(ChannelMessage.NewWindow, AppData()))
    }

    private fun itemNewWidgetFun() {
        TODO()
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
        TODO()
    }

    private fun itemToggleTitleFun() {
        appData.isUndecorated = !appData.isUndecorated
        appData.bounds = bounds
        TODO()
        channel.set(Channel(ChannelMessage.Reinit, appData))
        this.dispose()
    }

    private fun itemRemoveWidgetFun() {
        TODO()
    }

    private fun itemExitFun() {
        channel.set(Channel(ChannelMessage.Exit, AppData()))
        this.dispose()
    }
}