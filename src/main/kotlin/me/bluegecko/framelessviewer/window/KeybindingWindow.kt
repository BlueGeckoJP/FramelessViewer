package me.bluegecko.framelessviewer.window

import me.bluegecko.framelessviewer.App
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

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
        table.columnModel.getColumn(1).apply {
            cellRenderer = KeybindingRenderer()
            cellEditor = KeybindingEditor {
                TODO()
                /*
                JOptionPane.showMessageDialog(
                    this@KeybindingWindow,
                    "Button clicked",
                    "Button clicked",
                    JOptionPane.INFORMATION_MESSAGE,
                )
                 */
            }
        }

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

    inner class KeybindingRenderer : TableCellRenderer {
        private val label = JLabel()

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            label.text = value?.toString() ?: ""

            label.background = if (isSelected) table.selectionBackground else table.background

            label.apply {
                foreground = Color.CYAN.darker()
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
            }

            return label
        }
    }

    inner class KeybindingEditor(val onClick: () -> Unit) : DefaultCellEditor(JCheckBox()) {
        private val label = JLabel()

        init {
            label.apply {
                foreground = Color.CYAN
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        onClick()
                    }
                })
            }
        }

        override fun getTableCellEditorComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int
        ): Component {
            label.text = value?.toString() ?: ""
            label.border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
            return label
        }

        override fun getCellEditorValue(): Any = label.text
    }
}

data class TableItem(
    val functionName: String,
    val keybinding: String,
)