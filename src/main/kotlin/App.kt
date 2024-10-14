package me.bluegecko

import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.*
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class App(msg: AtomicReference<Channel>) : JFrame() {
    var focusedWidget: ImageWidget
    var imageWidgets: MutableList<ImageWidget> = mutableListOf()
    var appData = msg.get().initAppData
    private var channel = msg
    val popupMenu = PopupMenu(this)
    private val panel = JPanel()

    init {
        title = "FramelessViewer"
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isUndecorated = appData.isUndecorated
        bounds = appData.bounds

        appData.frameWidth = width - insets.left - insets.right
        appData.frameHeight = height - insets.top - insets.bottom

        val itemTitleBar = JMenuItem("Toggle titleBar")
        val itemNew = JMenuItem("New")
        val itemNewWindowWithImage = JMenuItem("New with same options")
        val itemNewWidget = JMenuItem("New widget")
        val itemOpen = JMenuItem("Open")
        val itemRemoveWidget = JMenuItem("Remove widget")
        val itemExit = JMenuItem("Exit")
        itemTitleBar.addActionListener { toggleTitleBar() }
        itemNew.addActionListener { new() }
        itemNewWindowWithImage.addActionListener { newWindowWithImage() }
        itemNewWidget.addActionListener { newWidget() }
        itemOpen.addActionListener { open() }
        itemRemoveWidget.addActionListener { removeWidget() }
        itemExit.addActionListener { exit() }
        popupMenu.add(itemTitleBar)
        popupMenu.add(itemNew)
        popupMenu.add(itemNewWindowWithImage)
        popupMenu.add(itemNewWidget)
        popupMenu.add(itemOpen)
        popupMenu.addSeparator()
        popupMenu.add(itemRemoveWidget)
        popupMenu.add(itemExit)

        panel.layout = GridBagLayout()

        if (appData.initPath.isNotEmpty()) {
            appData.imageDataList.add(ImageWidgetData(this, appData.initPath, this.width, this.height))
        }

        if (appData.imageDataList.isEmpty()) {
            addImageWidget()
        } else {
            val imageDataList = appData.imageDataList
            imageDataList.forEach {
                try {
                    addImageWidget(it.imagePath)
                } catch (e: Exception) {
                    println("An error occurred in initializing ImageWidget. Ignored.")
                    e.printStackTrace()
                }
            }
        }

        focusedWidget = imageWidgets[0]

        contentPane.add(panel, BorderLayout.CENTER)

        addKeyListener(ArrowKeyListener())
        addComponentListener(WindowResizeListener())
        addWindowListener(WindowEventListener())
    }

    inner class WindowResizeListener : ComponentListener {
        override fun componentHidden(p0: ComponentEvent?) {}
        override fun componentMoved(p0: ComponentEvent?) {}
        override fun componentShown(p0: ComponentEvent?) {}

        override fun componentResized(p0: ComponentEvent?) {
            appData.frameWidth = size.width - insets.left - insets.right
            appData.frameHeight = size.height - insets.top - insets.bottom

            imageWidgets.forEach {
                it.updateImage()
            }
        }
    }

    inner class WindowEventListener : WindowListener {
        override fun windowClosing(p0: WindowEvent?) {
            if (p0 != null) {
                channel.set(Channel(ChannelMessage.Exit, AppData()))
                this@App.dispose()
            }
        }

        override fun windowOpened(p0: WindowEvent?) {}
        override fun windowClosed(p0: WindowEvent?) {}
        override fun windowIconified(p0: WindowEvent?) {}
        override fun windowDeiconified(p0: WindowEvent?) {}
        override fun windowActivated(p0: WindowEvent?) {}
        override fun windowDeactivated(p0: WindowEvent?) {}
    }

    inner class ArrowKeyListener : KeyListener {
        override fun keyPressed(p0: KeyEvent?) {}
        override fun keyTyped(p0: KeyEvent?) {}
        override fun keyReleased(p0: KeyEvent?) {
            if (p0 != null) {
                if (p0.modifiersEx == KeyEvent.CTRL_DOWN_MASK) {
                    if (p0.keyCode == KeyEvent.VK_LEFT) {
                        try {
                            Collections.swap(
                                imageWidgets,
                                imageWidgets.indexOf(focusedWidget),
                                imageWidgets.indexOf(focusedWidget) - 1
                            )
                            repaintWidgets()
                        } catch (e: Exception) {
                            println("Cannot swap in that direction")
                        }
                    }
                    if (p0.keyCode == KeyEvent.VK_RIGHT) {
                        try {
                            Collections.swap(
                                imageWidgets,
                                imageWidgets.indexOf(focusedWidget),
                                imageWidgets.indexOf(focusedWidget) + 1
                            )
                            repaintWidgets()
                        } catch (e: Exception) {
                            println("Cannot swap in that direction")
                        }

                    }
                } else if (p0.modifiersEx == KeyEvent.SHIFT_DOWN_MASK) {
                    if (p0.keyCode == KeyEvent.VK_LEFT) {
                        if (focusedWidget.gridwidth > 1) {
                            focusedWidget.gridwidth -= 1
                            repaintWidgets()
                        }
                    } else if (p0.keyCode == KeyEvent.VK_RIGHT) {
                        focusedWidget.gridwidth += 1
                        repaintWidgets()
                    }
                } else if (focusedWidget.data.imagePath != "") {
                    val fileListIndex = focusedWidget.fileList.indexOf(focusedWidget.data.imagePath)
                    if (p0.keyCode == KeyEvent.VK_LEFT) {
                        if (fileListIndex - 1 < 0) {
                            focusedWidget.data.imagePath = focusedWidget.fileList[focusedWidget.fileList.size - 1]
                        } else {
                            focusedWidget.data.imagePath = focusedWidget.fileList[fileListIndex - 1]
                        }
                        focusedWidget.updateImage()
                    } else if (p0.keyCode == KeyEvent.VK_RIGHT) {
                        if (fileListIndex + 1 >= focusedWidget.fileList.size) {
                            focusedWidget.data.imagePath = focusedWidget.fileList[0]
                        } else {
                            focusedWidget.data.imagePath = focusedWidget.fileList[fileListIndex + 1]
                        }
                        focusedWidget.updateImage()
                    }
                    focusedWidget.updateTitle()
                }
            }
        }
    }

    private fun addImageWidget(path: String = "") {
        val imageData = ImageWidgetData(this, path, this.width, this.height)
        if (!appData.isUndecorated) {
            imageData.width = appData.frameWidth
            imageData.height = appData.frameHeight
        }
        val iw = ImageWidget(imageData)
        iw.horizontalAlignment = JLabel.CENTER
        iw.verticalAlignment = JLabel.CENTER

        imageWidgets.add(iw)
        addWidgetWithGBC(iw)

        panel.revalidate()
        panel.repaint()
    }

    private fun addWidgetWithGBC(widget: ImageWidget) {
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH
        gbc.gridx = getGridX(widget)
        gbc.gridy = 0
        gbc.weightx = getWeightX(widget)
        gbc.weighty = 1.0
        gbc.gridwidth = widget.gridwidth
        gbc.gridheight = 1

        panel.add(widget, gbc)
    }

    private fun getGridX(widget: ImageWidget): Int {
        var gridxSum = 0
        var index = 0
        while (imageWidgets[index] != widget) {
            gridxSum += imageWidgets[index].gridx
            gridxSum += imageWidgets[index].gridwidth - 1
            index++
        }
        return gridxSum + 1

    }

    private fun getWeightX(widget: ImageWidget): Double {
        val totalWidth = imageWidgets.sumOf { it.gridwidth }

        return if (totalWidth > 0) {
            widget.gridwidth.toDouble() / totalWidth
        } else {
            0.0
        }
    }

    private fun repaintWidgets() {
        panel.removeAll()

        imageWidgets.forEach { addWidgetWithGBC(it) }

        if (imageWidgets.isEmpty()) {
            addImageWidget()
        }

        panel.revalidate()
        panel.repaint()

        imageWidgets.forEach { it.updateImage() }
    }

    private fun toggleTitleBar() {
        appData.isUndecorated = !appData.isUndecorated
        appData.bounds = bounds
        appData.imageDataList = imageWidgets.map { it.data } as MutableList<ImageWidgetData>
        channel.set(Channel(ChannelMessage.Reinit, appData))
        this@App.dispose()
    }

    private fun new() {
        channel.set(Channel(ChannelMessage.NewWindow, AppData()))
    }

    private fun newWindowWithImage() {
        channel.set(
            Channel(
                ChannelMessage.NewWindowWithImage,
                AppData(imageDataList = imageWidgets.map { it.data } as MutableList<ImageWidgetData>)))
    }

    private fun newWidget() {
        addImageWidget()

        imageWidgets.forEach {
            it.updateImage()
        }
    }

    private fun open() {
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
            focusedWidget.data.imagePath = file.absolutePath
            repaintWidgets()
            imageWidgets.forEach { it.updateImage() }
        }
    }

    private fun removeWidget() {
        panel.remove(focusedWidget)
        imageWidgets.remove(focusedWidget)

        repaintWidgets()
    }

    private fun exit() {
        channel.set(Channel(ChannelMessage.Exit, AppData()))
        this@App.dispose()
    }
}