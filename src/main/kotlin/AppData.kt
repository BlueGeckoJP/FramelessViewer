package me.bluegecko

import java.awt.Rectangle

data class AppData(
    var isUndecorated: Boolean = true,
    var bounds: Rectangle = Rectangle(0, 0, 600, 400),
    var filePath: String = "",
    var frameWidth: Int = 0,
    var frameHeight: Int = 0,
    var fileList: MutableList<String> = mutableListOf(""),
    var fileListIndex: Int = 0,
    var extensionRegex: Regex = Regex(".jpg|.jpeg|.png|.webp")
)
