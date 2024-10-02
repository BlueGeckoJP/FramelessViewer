package me.bluegecko

import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class App(msg: AtomicReference<Channel>) : JFrame() {
    var focusedWidget: ImageWidget
    private var imageWidgets: MutableList<ImageWidget> = mutableListOf()
    var appData = msg.get().initAppData
    private var channel = msg
    val popupMenu = PopupMenu(this)
    private val panel = JPanel()
    private var gridx = 0

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
        val itemExit = JMenuItem("Exit")
        itemTitleBar.addActionListener { toggleTitleBar() }
        itemNew.addActionListener { new() }
        itemNewWindowWithImage.addActionListener { newWindowWithImage() }
        itemNewWidget.addActionListener { newWidget() }
        itemOpen.addActionListener { open() }
        itemExit.addActionListener { exit() }
        popupMenu.add(itemTitleBar)
        popupMenu.add(itemNew)
        popupMenu.add(itemNewWindowWithImage)
        popupMenu.add(itemNewWidget)
        popupMenu.add(itemOpen)
        popupMenu.add(itemExit)

        panel.layout = GridBagLayout()

        if (appData.imageDataList.isEmpty()) {
            val imageData = ImageWidgetData(this, "", this.width, this.height)
            if (!appData.isUndecorated) {
                imageData.width = appData.frameWidth
                imageData.height = appData.frameHeight
            }
            val iw = ImageWidget(imageData)
            imageWidgets.add(iw)
            val iconLabel = imageWidgets[imageWidgets.indexOf(iw)]
            iconLabel.horizontalAlignment = JLabel.CENTER
            iconLabel.verticalAlignment = JLabel.CENTER

            val gbc = GridBagConstraints()
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.fill = GridBagConstraints.BOTH
            gbc.weightx = 1.0
            gbc.weighty = 1.0
            gbc.gridwidth = 1
            panel.add(iconLabel, gbc)
            panel.revalidate()
            panel.repaint()
        } else {
            val imageDataList = appData.imageDataList
            imageDataList.forEach {
                try {
                    val imageData = ImageWidgetData(this, it.imagePath, this.width, this.height)
                    if (!appData.isUndecorated) {
                        imageData.width = appData.frameWidth
                        imageData.height = appData.frameHeight
                    }
                    val iw = ImageWidget(imageData)
                    imageWidgets.add(iw)
                    val iconLabel = imageWidgets[imageWidgets.indexOf(iw)]
                    iconLabel.horizontalAlignment = JLabel.CENTER
                    iconLabel.verticalAlignment = JLabel.CENTER

                    val gbc = GridBagConstraints()
                    gbc.gridx = gridx
                    gridx += 1
                    gbc.gridy = 0
                    gbc.fill = GridBagConstraints.BOTH
                    gbc.weightx = 1.0
                    gbc.weighty = 1.0
                    gbc.gridwidth = 1
                    panel.add(iconLabel, gbc)
                    panel.revalidate()
                    panel.repaint()
                } catch (e: Exception) {
                    println("An error occurred in initializing ImageWidget. Ignored.")
                    e.printStackTrace()
                }
            }
        }

        focusedWidget = imageWidgets[0]

        contentPane.add(panel, BorderLayout.CENTER)

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

            focusedWidget.updateImage()
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
        channel.set(Channel(ChannelMessage.NewWindowWithImage, AppData(filePath = appData.filePath, imageDataList = imageWidgets.map { it.data } as MutableList<ImageWidgetData>)))
    }

    private fun newWidget() {
        val imageData = ImageWidgetData(this, "", this.width, this.height)
        if (!appData.isUndecorated) {
            imageData.width = appData.frameWidth
            imageData.height = appData.frameHeight
        }
        val iw = ImageWidget(imageData)
        imageWidgets.add(iw)
        val iconLabel = imageWidgets[imageWidgets.indexOf(iw)]
        iconLabel.horizontalAlignment = JLabel.CENTER
        iconLabel.verticalAlignment = JLabel.CENTER

        val gbc = GridBagConstraints()
        gbc.gridx = gridx
        gridx += 1
        gbc.gridy = 0
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.gridwidth = 1
        panel.add(iconLabel, gbc)
        panel.revalidate()
        panel.repaint()
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
            focusedWidget.updateImage()
        }
    }

    private fun exit() {
        channel.set(Channel(ChannelMessage.Exit, AppData()))
        this@App.dispose()
    }
}
