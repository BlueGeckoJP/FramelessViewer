package me.bluegecko

import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.*
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class App(msg: AtomicReference<Channel>) : JFrame() {
    private var iconLabel: ImageWidget
    var appData = msg.get().initAppData
    private var channel = msg

    init {
        title = "FramelessViewer"
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isUndecorated = appData.isUndecorated
        bounds = appData.bounds

        appData.frameWidth = width - insets.left - insets.right
        appData.frameHeight = height - insets.top - insets.bottom

        val popupMenu = PopupMenu(this)
        val itemTitleBar = JMenuItem("Toggle titleBar")
        val itemNew = JMenuItem("New")
        val itemNewWindowWithImage = JMenuItem("New with same options")
        val itemOpen = JMenuItem("Open")
        val itemExit = JMenuItem("Exit")
        itemTitleBar.addActionListener { toggleTitleBar() }
        itemNew.addActionListener { new() }
        itemNewWindowWithImage.addActionListener { newWindowWithImage() }
        itemOpen.addActionListener { open() }
        itemExit.addActionListener { exit() }
        popupMenu.add(itemTitleBar)
        popupMenu.add(itemNew)
        popupMenu.add(itemNewWindowWithImage)
        popupMenu.add(itemOpen)
        popupMenu.add(itemExit)

        val imageData = ImageWidgetData(this, "", this.width, this.height)
        if (!appData.isUndecorated) {
            imageData.width = appData.frameWidth
            imageData.height = appData.frameHeight
        }
        iconLabel = ImageWidget(imageData)
        iconLabel.horizontalAlignment = JLabel.CENTER
        iconLabel.verticalAlignment = JLabel.CENTER

        val panel = JPanel()
        panel.layout = GridBagLayout()

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 0.1
        gbc.weighty = 0.1
        panel.add(iconLabel, gbc)

        contentPane.add(panel, BorderLayout.CENTER)

        addComponentListener(WindowResizeListener())
        addWindowListener(WindowEventListener())

        iconLabel.updateImage()
    }

    inner class WindowResizeListener : ComponentListener {
        override fun componentHidden(p0: ComponentEvent?) {}
        override fun componentMoved(p0: ComponentEvent?) {}
        override fun componentShown(p0: ComponentEvent?) {}

        override fun componentResized(p0: ComponentEvent?) {
            appData.frameWidth = size.width - insets.left - insets.right
            appData.frameHeight = size.height - insets.top - insets.bottom

            this@App.iconLabel.updateImage()
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
        channel.set(Channel(ChannelMessage.Reinit, appData))
        this@App.dispose()
    }

    private fun new() {
        channel.set(Channel(ChannelMessage.NewWindow, AppData()))
    }

    private fun newWindowWithImage() {
        channel.set(Channel(ChannelMessage.NewWindowWithImage, AppData(filePath = appData.filePath)))
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
            this@App.iconLabel.data.imagePath = file.absolutePath
            this@App.iconLabel.updateImage()
        }
    }

    private fun exit() {
        channel.set(Channel(ChannelMessage.Exit, AppData()))
        this@App.dispose()
    }
}
