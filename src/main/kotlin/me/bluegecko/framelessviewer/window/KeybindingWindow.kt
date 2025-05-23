package me.bluegecko.framelessviewer.window

import me.bluegecko.framelessviewer.App
import me.bluegecko.framelessviewer.data.KeyData
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class KeybindingWindow(private val app: App) : JDialog(app, "Keybinding Config | FramelessViewer", true) {
    private val columnNames = arrayOf("Function", "Keybinding")
    private var tableData: Array<Array<String>> = arrayOf()
    private var enteredKey: KeyData? = null
    private var isListening = false
    private var table: JTable
    private var tableModel: DefaultTableModel

    init {
        size = Dimension(400, 300)
        layout = BorderLayout()
        setLocationRelativeTo(app)

        updateTableData()

        tableModel = object : DefaultTableModel(tableData, columnNames) {
            override fun isCellEditable(row: Int, column: Int): Boolean = column == 1
        }
        table = JTable(tableModel)
        table.columnModel.getColumn(1).apply {
            cellRenderer = KeybindingRenderer()
            cellEditor = KeybindingEditor { row ->
                SwingUtilities.invokeLater {
                    val listener = object : KeyAdapter() {
                        override fun keyPressed(e: KeyEvent) {
                            this@KeybindingWindow.enteredKey =
                                KeyData(e.keyCode, e.isControlDown, e.isShiftDown, e.isAltDown)
                        }
                    }
                    table.addKeyListener(listener)

                    tableModel.setValueAt("Listening..", row, 1)
                    table.editingCanceled(null)
                    table.removeEditor()


                    val timer = Timer(100) { event ->
                        val key = enteredKey
                        if (key != null) {
                            SwingUtilities.invokeLater {
                                val keybindingMap = app.appKeymapsClass.keymapsMap
                                val runnableMap = app.appKeymapsClass.actionsMap
                                val runnableName = tableModel.getValueAt(row, 0).toString()
                                val runnable = runnableMap[runnableName]
                                if (runnable != null) {
                                    keybindingMap.remove(keybindingMap.entries.find { it.value == runnable }?.key)
                                    keybindingMap[key] = runnable
                                }

                                table.removeKeyListener(listener)
                                enteredKey = null
                                isListening = false

                                updateTableData()
                                tableData.forEachIndexed { index, rowData ->
                                    rowData.forEachIndexed { col, value ->
                                        tableModel.setValueAt(value, index, col)
                                    }
                                }
                                table.editingCanceled(null)
                                table.removeEditor()
                                table.repaint()
                                table.revalidate()
                            }
                            (event.source as Timer).stop()
                        }
                    }
                    if (!isListening) timer.start()

                    isListening = true
                    isFocusable = true
                }
            }
        }

        val scrollPane = JScrollPane(table)
        contentPane.add(scrollPane, BorderLayout.CENTER)
    }

    private fun updateTableData() {
        val keybindingMap = app.appKeymapsClass.keymapsMap
        val runnableMap = app.appKeymapsClass.actionsMap
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

    inner class KeybindingEditor(val onClick: (Int) -> Unit) : DefaultCellEditor(JCheckBox()) {
        private val label = JLabel()
        private var currentRow = -1

        init {
            label.apply {
                foreground = Color.CYAN
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        onClick(currentRow)
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
            currentRow = row
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