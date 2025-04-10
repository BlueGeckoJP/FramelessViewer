package me.bluegecko.framelessviewer

import me.bluegecko.framelessviewer.data.KeyData
import java.awt.Rectangle
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class AppKeymaps(private val app: App) : KeyAdapter() {
    private val actionsMap: MutableMap<String, Runnable> = mutableMapOf()
    private val keymapsMap: MutableMap<KeyData, Runnable> = mutableMapOf()
    private lateinit var defaultKeymaps: List<Pair<String, KeyData>>

    val arrowActionBaseFun = fun(x: Int, y: Int, w: Int, h: Int) {
        if (app.appData.get().isLocked) return

        app.focusedPanel.bounds = Rectangle(
            x, y, w, h
        )
        app.focusedPanel.updateImageSize()
    }

    init {
        definitionDefaultKeymaps()

        addHalfOfAllKeymaps()
        addHalfOfSelfKeymaps()
        addOtherKeymaps()

        defaultKeymaps.forEach { keymapsMap[it.second] = actionsMap[it.first]!! }
    }

    override fun keyPressed(e: KeyEvent) {
        if (app.appData.get().isLocked) return

        if (e.modifiersEx and KeyEvent.SHIFT_DOWN_MASK != 0) app.isPressedShiftKey = true
    }

    override fun keyReleased(e: KeyEvent) {
        if (app.isPressedShiftKey) app.isPressedShiftKey = false

        val key = KeyData(e.keyCode, e.isControlDown, e.isShiftDown, e.isAltDown)
        val action = keymapsMap[key]
        action?.run()
    }

    private fun definitionDefaultKeymaps() {
        defaultKeymaps = listOf(
            Pair("halfOfAllLeft", KeyData(KeyEvent.VK_LEFT, ctrl = true)),
            Pair("halfOfAllRight", KeyData(KeyEvent.VK_RIGHT, ctrl = true)),
            Pair("halfOfAllUp", KeyData(KeyEvent.VK_UP, ctrl = true)),
            Pair("halfOfAllDown", KeyData(KeyEvent.VK_DOWN, ctrl = true)),
            Pair("halfOfSelfLeft", KeyData(KeyEvent.VK_LEFT, alt = true)),
            Pair("halfOfSelfRight", KeyData(KeyEvent.VK_RIGHT, alt = true)),
            Pair("halfOfSelfUp", KeyData(KeyEvent.VK_UP, alt = true)),
            Pair("halfOfSelfDown", KeyData(KeyEvent.VK_DOWN, alt = true)),
            Pair("prevImage", KeyData(KeyEvent.VK_LEFT)),
            Pair("nextImage", KeyData(KeyEvent.VK_RIGHT)),
            Pair("maximizeImage", KeyData(KeyEvent.VK_UP)),
            Pair("changePanelDivisor", KeyData(KeyEvent.VK_DOWN)),
            Pair("focusNextPanel", KeyData(KeyEvent.VK_PAGE_UP)),
            Pair("focusPrevPanel", KeyData(KeyEvent.VK_PAGE_DOWN))
        )
    }

    private fun addHalfOfAllKeymaps() {
        actionsMap["halfOfAllLeft"] = Runnable {
            arrowActionBaseFun(
                0,
                app.focusedPanel.y,
                app.appWidth / app.panelDivisor,
                app.focusedPanel.height
            )
        }
        actionsMap["halfOfAllRight"] = Runnable {
            arrowActionBaseFun(
                app.appWidth - (app.appWidth / app.panelDivisor),
                app.focusedPanel.y,
                app.appWidth / app.panelDivisor,
                app.focusedPanel.height
            )
        }
        actionsMap["halfOfAllUp"] = Runnable {
            arrowActionBaseFun(
                app.focusedPanel.x,
                0,
                app.focusedPanel.width,
                app.appHeight / app.panelDivisor
            )
        }
        actionsMap["halfOfAllDown"] = Runnable {
            arrowActionBaseFun(
                app.focusedPanel.x,
                app.appHeight - (app.appHeight / app.panelDivisor),
                app.focusedPanel.width,
                app.appHeight / app.panelDivisor
            )
        }
    }

    private fun addHalfOfSelfKeymaps() {
        actionsMap["halfOfSelfLeft"] = Runnable {
            arrowActionBaseFun(
                app.focusedPanel.x,
                app.focusedPanel.y,
                app.focusedPanel.width / app.panelDivisor,
                app.focusedPanel.height
            )
        }
        actionsMap["halfOfSelfRight"] = Runnable {
            arrowActionBaseFun(
                app.focusedPanel.x + (app.focusedPanel.width / app.panelDivisor),
                app.focusedPanel.y,
                app.focusedPanel.width / app.panelDivisor,
                app.focusedPanel.height
            )
        }
        actionsMap["halfOfSelfUp"] = Runnable {
            arrowActionBaseFun(
                app.focusedPanel.x,
                app.focusedPanel.y,
                app.focusedPanel.width,
                app.focusedPanel.height / app.panelDivisor
            )
        }
        actionsMap["halfOfSelfDown"] = Runnable {
            arrowActionBaseFun(
                app.focusedPanel.x,
                app.focusedPanel.y + (app.focusedPanel.height / app.panelDivisor),
                app.focusedPanel.width,
                app.focusedPanel.height / app.panelDivisor
            )
        }
    }

    private fun addOtherKeymaps() {
        actionsMap["prevImage"] = Runnable {
            if (app.focusedPanel.imagePath.isEmpty()) return@Runnable
            val fileList = app.focusedPanel.fileList.toList()
            val fileListIndex = fileList.indexOf(app.focusedPanel.imagePath)
            if (fileListIndex - 1 < 0) {
                app.focusedPanel.imagePath = fileList[fileList.size - 1]
            } else {
                app.focusedPanel.imagePath = fileList[fileListIndex - 1]
            }
            app.focusedPanel.updateImage()
        }
        actionsMap["nextImage"] = Runnable {
            if (app.focusedPanel.imagePath.isEmpty()) return@Runnable
            val fileList = app.focusedPanel.fileList.toList()
            val fileListIndex = fileList.indexOf(app.focusedPanel.imagePath)
            if (fileListIndex + 1 >= fileList.size) {
                app.focusedPanel.imagePath = fileList[0]
            } else {
                app.focusedPanel.imagePath = fileList[fileListIndex + 1]
            }
            app.focusedPanel.updateImage()
        }
        actionsMap["maximizeImage"] = Runnable {
            app.focusedPanel.bounds = Rectangle(0, 0, app.appWidth, app.appHeight)
            app.focusedPanel.updateImageSize()
        }
        actionsMap["changePanelDivisor"] = Runnable {
            app.panelDivisor = 5 - app.panelDivisor
            app.updateTitle()
        }
        actionsMap["focusNextPanel"] = Runnable {
            if (app.appData.get().isLocked) return@Runnable

            val panels = app.getPanels()
            val index = panels.indexOf(app.focusedPanel)

            if (index + 1 >= panels.size) app.focusToPanel(panels[0])
            else app.focusToPanel(panels[index + 1])
        }
        actionsMap["focusPrevPanel"] = Runnable {
            if (app.appData.get().isLocked) return@Runnable

            val panels = app.getPanels()
            val index = panels.indexOf(app.focusedPanel)

            if (index - 1 < 0) app.focusToPanel(panels[panels.size - 1])
            else app.focusToPanel(panels[index - 1])
        }
    }
}