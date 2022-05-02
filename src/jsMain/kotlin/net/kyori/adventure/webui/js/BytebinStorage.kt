package net.kyori.adventure.webui.js

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import net.kyori.adventure.webui.Serializers
import net.kyori.adventure.webui.tryDecodeFromString
import net.kyori.adventure.webui.websocket.Combined
import net.kyori.adventure.webui.websocket.Placeholders
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.WebSocket
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

private const val BYTEBIN_INSTANCE: String = "https://bytebin.lucko.me"

public fun bytebinStore(payload: Combined): Promise<String?> {
    return window.fetch(
        "$BYTEBIN_INSTANCE/post",
        RequestInit(
            method = "POST",
            headers = Headers(json("Content-Type" to "application/json; charset=UTF-8")),
            body = Serializers.json.encodeToString(payload)
        )
    )
        .then { response -> response.json() }
        .then { json -> json.unsafeCast<Json>()["key"].unsafeCast<String>() } // :I
}

private fun bytebinLoad(code: String): Promise<Combined?> {
    // TODO(rymiel): handle the 404 case
    return window.fetch(
        "$BYTEBIN_INSTANCE/$code",
        RequestInit(method = "GET")
    )
        .then { response -> response.text() }
        .then { text -> Serializers.json.tryDecodeFromString(text) }
}

// TODO(rymiel): This probably shouldn't return a promise...?
// The fact that this makes an additional web request probably causes weird delayed jumps in the output once it loads?
// TODO(rymiel): Perhaps it could show some loading thing while it fetches the data for a short code
public fun restoreFromShortLink(shortCode: String, inputBox: HTMLTextAreaElement, webSocket: WebSocket): Promise<Unit> {
    return bytebinLoad(shortCode).then { structure ->
        // This is rather duplicated from Main.kt :(
        structure.getFromCombinedOrLocalStorage(PARAM_INPUT, Combined::miniMessage)?.also { inputString ->
            inputBox.value = inputString
        }
        val stringPlaceholders = structure.getFromCombinedOrLocalStorage(
            PARAM_STRING_PLACEHOLDERS,
            { c -> c.placeholders?.stringPlaceholders },
            { str -> Serializers.json.tryDecodeFromString(str) } // WTF
        )
        stringPlaceholders?.forEach { (k, v) ->
            UserPlaceholder.addToList().apply {
                key = k
                value = v
            }
        }
        structure.getFromCombinedOrLocalStorage(PARAM_BACKGROUND, Combined::background)?.also { background ->
            currentBackground = background
        }
        structure.getFromCombinedOrLocalStorage(PARAM_MODE, Combined::mode)?.also { mode ->
            setMode(Mode.fromString(mode))
        }
        webSocket.send(
            Placeholders(stringPlaceholders = stringPlaceholders)
        )
    }
}
