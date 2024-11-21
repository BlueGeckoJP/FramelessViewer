package me.bluegecko.framelessviewer.window

import me.bluegecko.framelessviewer.App
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyEvent
import javax.swing.JDialog
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class KeybindingWindow(private val app: App) : JDialog(app, "Keybinding Config | FramelessViewer", true) {
    private val columnNames = arrayOf("Function", "Keybinding")
    private var tableData: Array<Array<String>> = arrayOf()

    init {
        size = Dimension(400, 300)
        layout = BorderLayout()
        setLocationRelativeTo(app)

        updateTableData()

        val tableModel = DefaultTableModel(tableData, columnNames)
        val table = JTable(tableModel)

        val scrollPane = JScrollPane(table)
        contentPane.add(scrollPane, BorderLayout.CENTER)
    }

    private fun updateTableData() {
        val keybindingMap = app.appKeyAdapter.keybindingMap
        val runnableMap = app.appKeyAdapter.runnableMap
        val tableItemList: MutableList<TableItem> = mutableListOf()
        keybindingMap.forEach {
            val functionName = runnableMap.entries.find { entry -> entry.value == it.value }?.key
            var keybinding = KeyEvent.getKeyText(it.key.keyCode)
            if (it.key.alt) keybinding = "Alt+$keybinding"
            if (it.key.shift) keybinding = "Shift+$keybinding"
            if (it.key.ctrl) keybinding = "Ctrl+$keybinding"

            if (functionName != null) {
                tableItemList.add(TableItem(functionName, keybinding))
            }
        }

        tableData = tableItemList.sortedBy { it.functionName }.map {
            arrayOf(it.functionName, it.keybinding)
        }.toTypedArray()
    }
}

data class TableItem(
    val functionName: String,
    val keybinding: String,
)