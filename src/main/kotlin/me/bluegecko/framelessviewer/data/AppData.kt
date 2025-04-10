package me.bluegecko.framelessviewer.data

import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.Rectangle

class AppData(innerAppData: InnerAppData = InnerAppData()) {
    var data: MutableStateFlow<InnerAppData> = MutableStateFlow(innerAppData)

    fun get(): InnerAppData {
        return data.value.copy()
    }

    inline fun applyData(block: InnerAppData.() -> Unit) {
        data.value.apply(block)
    }
}

data class InnerAppData(
    var isUndecorated: Boolean = false,
    var bounds: Rectangle = Rectangle(0, 0, 600, 400),
    var initPath: String = "",
    var panelDataList: MutableList<ImagePanelData> = mutableListOf(),
    var isLocked: Boolean = true
)
