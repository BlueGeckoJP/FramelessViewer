package me.bluegecko.framelessviewer.data

data class KeyData(
    val keyCode: Int,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
)
