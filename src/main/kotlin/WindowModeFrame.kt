package me.bluegecko

import java.awt.Color
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.abs

class WindowModeFrame : JFrame() {
    val snapDistance = 20

    private val panel = JPanel()
    private var initClick = Point()

    init {
        title = "Frameless Viewer"
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = null

        panel.background = Color.GRAY
        panel.bounds = Rectangle(600, 400)

        add(panel)

        val listener = DraggableListener()
        panel.addMouseListener(listener)
        panel.addMouseMotionListener(listener)
    }

    inner class DraggableListener : MouseAdapter() {
        override fun mousePressed(e: MouseEvent?) {
            if (e != null) {
                initClick = e.point
            }
        }

        override fun mouseDragged(e: MouseEvent?) {
            if (e != null) {
                val panelX = panel.x
                val panelY = panel.y
                val mouseX = e.x + panelX
                val mouseY = e.y + panelY

                var newX = panelX + (mouseX - initClick.x - panelX)
                var newY = panelY + (mouseY - initClick.y - panelY)

                newX = snap(newX, width - panel.width)
                newY = snap(newY, height - panel.height)

                panel.location = Point(newX, newY)
            }
        }

        private fun snap(position: Int, max: Int): Int {
            if (abs(position) < snapDistance) return 0
            if (abs(position - max) < snapDistance) return max
            return position
        }
    }
}