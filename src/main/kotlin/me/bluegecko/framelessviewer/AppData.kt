package me.bluegecko.framelessviewer

import java.awt.Rectangle

data class AppData(
    var isUndecorated: Boolean = false,
    var bounds: Rectangle = Rectangle(0, 0, 600, 400),
    var initPath: String = "",
    var frameWidth: Int = 0,
    var frameHeight: Int = 0,
    var imageDataList: MutableList<ImageWidgetData> = mutableListOf(),
    var panelDataList: MutableList<PanelData> = mutableListOf()
)