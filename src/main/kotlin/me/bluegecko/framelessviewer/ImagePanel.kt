package me.bluegecko.framelessviewer

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.LineBorder
import kotlin.math.abs

class ImagePanel(val app: App, path: String = "") : JPanel() {
    val widget: ImageWidget

    init {
        border = LineBorder(app.defaultColor, 1)
        background = Color.GRAY
        bounds = Rectangle(600, 400)
        layout = GridBagLayout()

        widget = ImageWidget(ImageWidgetData(app, path, app.appWidth, app.appHeight))

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.gridwidth = 1
        gbc.gridheight = 1

        add(widget, gbc)

        val listener = DraggableListener()
        addMouseListener(listener)
        addMouseMotionListener(listener)

        widget.updateImage()
    }

    inner class DraggableListener : MouseAdapter() {
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
                var newX = snapToEdge(
                    this@ImagePanel.x + e.x - initClick.x,
                    this@ImagePanel.parent.width - this@ImagePanel.width
                )
                var newY = snapToEdge(
                    this@ImagePanel.y + e.y - initClick.y,
                    this@ImagePanel.parent.height - this@ImagePanel.height
                )

                val sto = snapToOther(
                    newX, newY,
                    this@ImagePanel.width,
                    this@ImagePanel.height
                )

                if (sto != null) {
                    newX = sto.first
                    newY = sto.second
                }

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
            return position
        }

        private fun snapToOther(x: Int, y: Int, width: Int, height: Int): Pair<Int, Int>? {
            for (component in app.getPanels()) {
                if (component === this@ImagePanel) continue

                val other = component.bounds
                var newX = x
                var newY = y

                if ((other.y <= y && y <= other.height + other.y) || (other.y <= y + height && y + height <= other.height + other.y) || (y < other.y && other.y + other.height < y + height)) {
                    if (abs(x - other.x + width) < snapDistance) newX = other.x - width
                    if (abs(x - (other.x + other.width)) < snapDistance) newX = other.x + other.width
                }
                if ((other.x <= x && x <= other.width + other.x) || (other.x <= x + width && x + width <= other.width + other.x) || (x <= other.x && other.x + other.width < x + width)) {
                    if (abs(y - other.y + height) < snapDistance) newY = other.y - height
                    if (abs(y - (other.y + other.height)) < snapDistance) newY = other.y + other.height
                }

                if (newX != x || newY != y) return newX to newY
            }
            return null
        }

        private fun isNearCorner(x: Int, y: Int): Boolean {
            return (x in 0 until snapDistance || x in this@ImagePanel.width - snapDistance until this@ImagePanel.width) &&
                    (y in 0 until snapDistance || y in this@ImagePanel.height - snapDistance until this@ImagePanel.height)
        }
    }
}
