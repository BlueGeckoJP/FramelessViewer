package me.bluegecko.framelessviewer

data class KeyInfo(
    val keyCode: Int,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
)
