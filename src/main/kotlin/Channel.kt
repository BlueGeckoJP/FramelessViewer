package me.bluegecko

data class Channel(var message: ChannelMessage, var initAppData: AppData)

enum class ChannelMessage {
    Normal,
    Exit,
    NewWindow,
    Reinit,
    NewWindowWithImage
}
