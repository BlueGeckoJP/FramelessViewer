package me.bluegecko

import java.awt.Color
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.border.LineBorder
import kotlin.math.abs

open class WindowModeFrame : JFrame() {
    val snapDistance = 20

    init {
        layout = null
    }

    inner class DraggableListener(panel: JPanel) : MouseAdapter() {
        private var targetPanel: JPanel = panel
        private lateinit var initClick: Point

        override fun mousePressed(e: MouseEvent?) {
            if (e != null) {
                initClick = e.point
            }
        }

        override fun mouseDragged(e: MouseEvent?) {
            if (e != null) {
                val panelX = targetPanel.x
                val panelY = targetPanel.y
                val mouseX = e.x + panelX
                val mouseY = e.y + panelY

                var newX = panelX + (mouseX - initClick.x - panelX)
                var newY = panelY + (mouseY - initClick.y - panelY)

                newX = snap(newX, width - targetPanel.width)
                newY = snap(newY, height - targetPanel.height)

                targetPanel.location = Point(newX, newY)
            }
        }

        private fun snap(position: Int, max: Int): Int {
            if (abs(position) < snapDistance) return 0
            if (abs(position - max) < snapDistance) return max
            return position
        }
    }

    fun createDraggablePanel(x: Int, y: Int): JPanel {
        val panel = JPanel()

        panel.border = LineBorder(Color.GRAY, 1)
        panel.bounds = Rectangle(600, 400)

        val listener = DraggableListener(panel)
        panel.addMouseListener(listener)
        panel.addMouseMotionListener(listener)

        return panel
    }
}