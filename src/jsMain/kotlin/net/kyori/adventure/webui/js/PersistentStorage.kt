package net.kyori.adventure.webui.js

import kotlinx.browser.window
import net.kyori.adventure.webui.websocket.Combined
import org.w3c.dom.get
import org.w3c.dom.url.URLSearchParams

/** Fetches an input passed from URL if there is one, otherwise tries to use local storage */
public fun URLSearchParams.getFromParamsOrLocalStorage(key: String): String? {
    val shared = this.get(key)
    if (shared != null) {
        println("SHARED $key: $shared")
        return shared
    } else {
        window.localStorage[key]?.also { stored ->
            println("STORED $key: $stored")
            return stored
        }
    }
    return null
}

/** Fetches an input passed through a Combined if it is storing one, otherwise tries to use local storage */
public fun <T> Combined?.getFromCombinedOrLocalStorage(key: String, method: (Combined) -> T?, mapping: (String) -> T = { it.unsafeCast<T>() }): T? {
    val shared = if (this != null) method.invoke(this) else null
    if (shared != null) {
        println("SHARED $key: $shared")
        return shared
    } else {
        window.localStorage[key]?.also { stored ->
            println("STORED $key: $stored")
            return mapping.invoke(stored)
        }
    }
    return null
}
