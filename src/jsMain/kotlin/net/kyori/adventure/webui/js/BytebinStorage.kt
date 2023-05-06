package net.kyori.adventure.webui.js

import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.kyori.adventure.webui.BuildInfo
import net.kyori.adventure.webui.Serializers
import net.kyori.adventure.webui.URL_API
import net.kyori.adventure.webui.URL_BUILD_INFO
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

private lateinit var BYTEBIN_INSTANCE: String

private fun retrieveBytebinUrl(): Promise<String> {
    if (::BYTEBIN_INSTANCE.isInitialized) {
        return Promise.resolve(BYTEBIN_INSTANCE)
    }

    return window.fetch(
        "$URL_API$URL_BUILD_INFO",
        RequestInit(method = "GET")
    )
        .then { response -> response.text() }
        .then { text -> Serializers.json.decodeFromString<BuildInfo>(text) }
        .then { buildInfo ->
            BYTEBIN_INSTANCE = buildInfo.bytebinInstance
            BYTEBIN_INSTANCE
        }
}

public fun bytebinStore(payload: Combined): Promise<String?> {
    return retrieveBytebinUrl().then { bytebinInstance ->
        // TODO(rymiel): maybe this can fail?
        window.fetch(
            "$bytebinInstance/post",
            RequestInit(
                method = "POST",
                headers = Headers(json("Content-Type" to "application/json; charset=UTF-8")),
                body = Serializers.json.encodeToString(payload)
            )
        )
    }
        .then { response -> response.json() }
        .then { json -> json.unsafeCast<Json>()["key"].unsafeCast<String>() } // :I
}

private fun bytebinLoad(code: String): Promise<Combined?> {
    return retrieveBytebinUrl().then { bytebinInstance ->
        window.fetch(
            "$bytebinInstance/$code",
            RequestInit(method = "GET")
        )
    }
        .then { response -> response.text() }
        .then(
            { text -> Serializers.json.tryDecodeFromString(text) },
            { null } // Look, mom! I handled the error!
        )
}

// TODO(rymiel): This probably shouldn't return a promise...?
// The fact that this makes an additional web request probably causes weird delayed jumps in the output once it loads?
// TODO(rymiel): Perhaps it could show some loading thing while it fetches the data for a short code. This could apply to the site loading as a whole, it's not exactly blazing fast
public fun restoreFromShortLink(shortCode: String, inputBox: HTMLTextAreaElement, webSocket: WebSocket): Promise<Unit> {
    return bytebinLoad(shortCode).then { structure ->
        if (structure == null) {
            bulmaToast.toast("Failed to load from the short link! The link may have expired.", type = "is-danger")
        }
        // This is rather duplicated from Main.kt :(
        /*
        TODO(rymiel): since the `Combined` class is really being abused here, since that was made for websocket communication, this should be separated out to its own data class,
          which could then implement a common interface or something with what's being done in Main.kt
         */
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
        structure.getFromCombinedOrLocalStorage(PARAM_DOWNSAMPLE, Combined::downsampler)?.also { downsampler ->
            currentDownsampler = downsampler
        }
        webSocket.send(
            Placeholders(stringPlaceholders = stringPlaceholders)
        )
    }
}
