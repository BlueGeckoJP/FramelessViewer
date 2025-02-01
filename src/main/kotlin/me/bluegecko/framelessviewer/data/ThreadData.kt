package me.bluegecko.framelessviewer.data

import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicReference

data class ThreadData(
    val uuid: String,
    val thread: Job,
    val channel: AtomicReference<Channel>
)

data class Channel(
    var message: ChannelMessage = ChannelMessage.Normal,
    var appData: AppData = AppData(),
    var sendImagePath: String = "",
    var sendImageTo: String = "",
    var isReceived: Boolean = false,
    var receivedImagePath: String = ""
)

enum class ChannelMessage {
    Normal,
    Exit,
    NewWindow,
    Reinit,
    NewWindowWithImage,
    SendImage
}
