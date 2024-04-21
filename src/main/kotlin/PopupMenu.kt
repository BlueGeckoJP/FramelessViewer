package me.bluegecko

import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPopupMenu


class PopupMenu(component: Component) : JPopupMenu() {
    init {
        component.addMouseListener(PopupListener())
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
}