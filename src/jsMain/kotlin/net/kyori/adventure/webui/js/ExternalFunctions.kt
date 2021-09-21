// this entire file is everything that is bad about kotlin/js
@file:Suppress("ClassName")

package net.kyori.adventure.webui.js

import kotlin.js.Json

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
