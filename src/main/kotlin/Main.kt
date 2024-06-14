package me.bluegecko

fun main() {
    var app = App()
    app.isVisible = true
    while (true) {
        if (!app.isVisible) {
            app.dispose()
            app = App()
            app.isVisible = true
        }
    }
}
