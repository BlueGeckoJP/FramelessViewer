package me.bluegecko.framelessviewer.data

import java.awt.Rectangle

data class AppData(
    var isUndecorated: Boolean = false,
    var bounds: Rectangle = Rectangle(0, 0, 600, 400),
    var initPath: String = "",
    var panelDataList: MutableList<ImagePanelData> = mutableListOf(),
    var isLocked: Boolean = true
)
