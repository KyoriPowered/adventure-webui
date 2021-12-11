package net.kyori.adventure.webui.js

import kotlinx.browser.window
import org.w3c.dom.get
import org.w3c.dom.url.URLSearchParams

/** Fetches an input passed from URL if there is one, otherwise tries to use local storage */
public fun URLSearchParams.getFromParamsOrLocalStorage(key: String): String? {
    val shared = this.get(key)
    if (shared != null) {
        val text = decodeURIComponent(shared)
        println("SHARED $key: $text")
        return text
    } else {
        window.localStorage[key]?.also { stored ->
            println("STORED $key: $stored")
            return stored
        }
    }
    return null
}
