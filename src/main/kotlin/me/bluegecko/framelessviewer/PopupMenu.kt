package me.bluegecko.framelessviewer

import me.bluegecko.framelessviewer.data.Channel
import me.bluegecko.framelessviewer.data.ChannelMessage
import me.bluegecko.framelessviewer.window.KeybindingWindow
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFileChooser
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.event.MenuEvent
import javax.swing.event.MenuListener
import javax.swing.filechooser.FileNameExtensionFilter


class PopupMenu(val app: App) : JPopupMenu() {
    init {
        app.addMouseListener(PopupListener())

        val itemNew = JMenuItem("New")
        val itemNewWidget = JMenuItem("New Widget")
        val itemOpen = JMenuItem("Open")
        val itemClone = JMenuItem("Clone")
        val itemLock = JMenuItem("Lock To Window")
        val itemToggleTitle = JMenuItem("Toggle Title")
        val itemFitToImage = JMenuItem("Fit To Image")
        val itemSetZoomRatioToAuto = JMenuItem("Set Zoom Ratio To Auto")
        val itemOpenKeybindingWindow = JMenuItem("Open Keybinding Window")
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
        itemOpenKeybindingWindow.addActionListener { itemOpenKeybindingWindowFun() }
        itemRemoveWidget.addActionListener { itemRemoveWidgetFun() }
        itemExit.addActionListener { itemExitFun() }

        val menuSendImageTo = JMenu("Send Image To")
        menuSendImageTo.addMenuListener(object : MenuListener {
            override fun menuSelected(e: MenuEvent) {
                menuSendImageTo.removeAll()

                appController.getThreadUUIDs().forEach {
                    if (it != app.uuid) {
                        val otherUUID = it
                        val item = JMenuItem(appController.getShortUUID(otherUUID))
                        item.addActionListener { sendImageTo(otherUUID) }
                        menuSendImageTo.add(item)
                    }
                }
            }

            override fun menuDeselected(e: MenuEvent?) {}
            override fun menuCanceled(e: MenuEvent?) {}
        })

        this.add(itemNew)
        this.add(itemNewWidget)
        this.addSeparator()
        this.add(itemOpen)
        this.add(itemClone)
        this.addSeparator()
        this.add(itemLock)
        this.add(itemToggleTitle)
        this.add(itemFitToImage)
        this.add(itemSetZoomRatioToAuto)
        this.add(menuSendImageTo)
        this.addSeparator()
        this.add(itemOpenKeybindingWindow)
        this.addSeparator()
        this.add(itemRemoveWidget)
        this.add(itemExit)
    }

    inner class PopupListener : MouseAdapter() {
        override fun mousePressed(e: MouseEvent?) {
            showPopup(e)
        }

        override fun mouseReleased(e: MouseEvent?) {
            showPopup(e)
        }

        private fun showPopup(e: MouseEvent?) {
            if (e != null) {
                if (e.isPopupTrigger) {
                    this@PopupMenu.show(e.component, e.x, e.y)
                }
            }
        }
    }

    private fun itemNewFun() {
        app.channel.set(Channel(ChannelMessage.NewWindow))
    }

    private fun itemNewWidgetFun() {
        app.createNewPanel()
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
            app.focusedPanel.setImagePath(file.absolutePath)
            app.focusedPanel.zoomRatio = 1.0
            app.focusedPanel.updateImage()
        }
    }

    private fun itemCloneFun() {
        app.updateAppData()
        app.channel.set(Channel(ChannelMessage.NewWindowWithImage))
    }

    private fun itemLockFun() {
        app.appData.applyData { isLocked = !app.appData.get().isLocked }
        app.focusedPanel.border =
            if (app.appData.get().isLocked) EmptyBorder(0, 0, 0, 0) else LineBorder(app.focusedColor, 1)
        app.focusedPanel.bounds = Rectangle(0, 0, app.innerSize.width, app.innerSize.height)

        repaint()
        revalidate()
    }

    private fun itemFitToImageFun() {
        app.focusedPanel.bounds =
            Rectangle(
                app.focusedPanel.x,
                app.focusedPanel.y,
                app.focusedPanel.resizedWidth,
                app.focusedPanel.resizedHeight
            )
        app.focusedPanel.zoomRatio = 1.0
        app.focusedPanel.translateX = 0
        app.focusedPanel.translateY = 0
        app.focusedPanel.updateImageSize()
    }

    private fun itemSetZoomRatioToAutoFun() {
        app.focusedPanel.resizedWidth = app.focusedPanel.image.width
        app.focusedPanel.resizedHeight = app.focusedPanel.image.height
        app.focusedPanel.repaint()
        app.focusedPanel.revalidate()
    }

    private fun itemToggleTitleFun() {
        app.updateAppData()
        app.appData.applyData { isUndecorated = !app.appData.get().isUndecorated }
        app.channel.set(Channel(ChannelMessage.Reinit))
        app.dispose()
    }

    private fun itemOpenKeybindingWindowFun() {
        val window = KeybindingWindow(app)
        window.isVisible = true
    }

    private fun itemRemoveWidgetFun() {
        app.remove(app.focusedPanel)

        app.appData.applyData { isLocked = false }

        if (app.getPanels().isEmpty()) {
            app.createNewPanel()
        }

        app.focusToPanel(app.getPanels()[0])

        repaint()
        revalidate()
    }

    private fun itemExitFun() {
        app.channel.set(Channel(ChannelMessage.Exit))
        app.dispose()
    }

    private fun sendImageTo(target: String) {
        val uuids = appController.getThreadUUIDs()
        if (uuids.contains(target)) {
            app.channel.set(
                Channel(
                    message = ChannelMessage.SendImage,
                    sendImageTo = target,
                    sendImagePath = app.focusedPanel.getImagePath()
                )
            )
        }
    }
}