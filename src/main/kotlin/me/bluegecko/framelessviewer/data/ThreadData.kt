package me.bluegecko.framelessviewer.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicReference

data class ThreadData(
    val uuid: String,
    val thread: Job,
    val channel: AtomicReference<Channel>,
    val appData: MutableStateFlow<AppData>,
)

data class Channel(
    var message: ChannelMessage = ChannelMessage.Normal,
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
