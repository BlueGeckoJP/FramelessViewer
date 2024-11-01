package me.bluegecko.framelessviewer

import java.util.*
import java.util.concurrent.atomic.AtomicReference

data class ThreadData(
    val uuid: String = UUID.randomUUID().toString(),
    val thread: Thread,
    val channel: AtomicReference<Channel>
)

data class Channel(
    var message: ChannelMessage = ChannelMessage.Normal,
    var appData: AppData = AppData()
)

enum class ChannelMessage {
    Normal,
    Exit,
    NewWindow,
    Reinit,
    NewWindowWithImage
}
