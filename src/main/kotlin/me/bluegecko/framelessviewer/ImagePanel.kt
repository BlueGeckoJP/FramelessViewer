package me.bluegecko.framelessviewer

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.LineBorder
import kotlin.math.abs

class ImagePanel(val app: App) : JPanel() {
    init {
        border = LineBorder(app.defaultColor, 1)
        background = Color.GRAY
        bounds = Rectangle(600, 400)

        val listener = DraggableListener()
        addMouseListener(listener)
        addMouseMotionListener(listener)
    }

    inner class DraggableListener() : MouseAdapter() {
        private val snapDistance = 20
        private val minimumSize = 50

        private lateinit var initClick: Point

        override fun mousePressed(e: MouseEvent) {
            initClick = e.point

            if (!app.isLocked) app.focusToPanel(this@ImagePanel)

            if (SwingUtilities.isRightMouseButton(e)) {
                app.popupMenu.show(e.component, e.x, e.y)
            } else if (isNearCorner(e.x, e.y)) {
                if (app.isLocked) return

                this@ImagePanel.cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)
            }
        }

        override fun mouseDragged(e: MouseEvent) {
            if (app.isLocked) return

            if (this@ImagePanel.cursor.type == Cursor.SE_RESIZE_CURSOR) {
                val newWidth = snapToEdge(e.x, this@ImagePanel.parent.width - this@ImagePanel.x)
                val newHeight = snapToEdge(e.y, this@ImagePanel.parent.height - this@ImagePanel.y)
                this@ImagePanel.size = Dimension(maxOf(newWidth, minimumSize), maxOf(newHeight, minimumSize))
            } else {
                val newX = snapToEdge(
                    this@ImagePanel.x + e.x - initClick.x,
                    this@ImagePanel.parent.width - this@ImagePanel.width
                )
                val newY = snapToEdge(
                    this@ImagePanel.y + e.y - initClick.y,
                    this@ImagePanel.parent.height - this@ImagePanel.height
                )
                this@ImagePanel.location = Point(newX, newY)
            }
            this@ImagePanel.repaint()
            this@ImagePanel.revalidate()
        }

        override fun mouseReleased(e: MouseEvent) {
            this@ImagePanel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
            app.getWidget(this@ImagePanel).updateImageSize()
        }

        private fun snapToEdge(position: Int, max: Int): Int {
            if (abs(position) < snapDistance) return 0
            if (abs(position - max) < snapDistance) return max

            for (component in app.components) {
                if (component === this@ImagePanel) continue

                val other = component.bounds
                if (abs(position - other.x) < snapDistance) return other.x
                if (abs(position - (other.x + other.width)) < snapDistance) return other.x + other.width
                if (abs(position - other.y) < snapDistance) return other.y
                if (abs(position - (other.y + other.height)) < snapDistance) return other.y + other.height
            }

            return position
        }

        private fun isNearCorner(x: Int, y: Int): Boolean {
            return (x in 0 until snapDistance || x in this@ImagePanel.width - snapDistance until this@ImagePanel.width) &&
                    (y in 0 until snapDistance || y in this@ImagePanel.height - snapDistance until this@ImagePanel.height)
        }
    }
}
