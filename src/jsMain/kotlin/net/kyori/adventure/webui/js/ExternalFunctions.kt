// this entire file is everything that is bad about kotlin/js
@file:Suppress("ClassName")

package net.kyori.adventure.webui.js

import kotlinx.browser.document
import kotlinx.html.DIV
import kotlinx.html.div
import kotlinx.html.dom.create
import kotlin.js.Json
import kotlin.js.json

// standard js methods
public external fun decodeURIComponent(encodedURI: String): String

public external fun encodeURIComponent(string: String): String

// bulma toast
public external class bulmaToast {
    public companion object {
        public fun toast(settings: Json)
        public fun setDefaults(settings: Json)
    }
}

public fun bulmaToast.Companion.toast(message: String, type: String = "is-success") {
    this.toast(json("message" to message, "type" to type))
}

public inline fun bulmaToast.Companion.toast(type: String = "is-success", crossinline block: DIV.() -> Unit) {
    val element = document.create.div(block = block)
    this.toast(json("message" to element, "type" to type))
}
