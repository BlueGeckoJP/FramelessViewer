package me.bluegecko

import java.awt.event.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.filechooser.FileNameExtensionFilter

class App(private val channel: AtomicReference<Channel>) : WindowModeFrame() {
    var appData = channel.get().initAppData
    var panels = arrayListOf<JPanel>()
    lateinit var focusedPanel: JPanel
    private var appWidth = this.width
    private var appHeight = this.height

    init {
        title = "FramelessViewer"
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isUndecorated = appData.isUndecorated
        bounds = appData.bounds

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

        val popupMenu = PopupMenu(this)
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
            panels.forEach { (it.getComponent(0) as ImageWidget).updateImage() }
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

    private fun updateAppSize() {
        if (!appData.isUndecorated) {
            appWidth = width - insets.left - insets.right
            appHeight = height - insets.top - insets.bottom
        }
    }

    private fun createNewPanel(path: String = "") {
        val panel = createDraggablePanel(appWidth, appHeight)
        val widget = ImageWidget(ImageWidgetData(this, path, appWidth, appHeight))
        panel.add(widget)
        panels.add(panel)
        this.add(panel)
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

        TODO()
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