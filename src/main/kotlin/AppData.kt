package me.bluegecko

import java.awt.Rectangle

data class AppData(
    var isUndecorated: Boolean,
    var bounds: Rectangle,
    var filePath: String,
    var frameWidth: Int,
    var frameHeight: Int,
    var fileList: MutableList<String>,
    var fileListIndex: Int,
    var extensionRegex: Regex
)
