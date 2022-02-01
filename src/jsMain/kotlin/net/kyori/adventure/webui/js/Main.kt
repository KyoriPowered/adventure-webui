package net.kyori.adventure.webui.js

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.hasClass
import kotlinx.serialization.encodeToString
import net.kyori.adventure.webui.COMPONENT_CLASS
import net.kyori.adventure.webui.DATA_CLICK_EVENT_ACTION
import net.kyori.adventure.webui.DATA_CLICK_EVENT_VALUE
import net.kyori.adventure.webui.DATA_INSERTION
import net.kyori.adventure.webui.PARAM_EDITOR_TOKEN
import net.kyori.adventure.webui.Serializers
import net.kyori.adventure.webui.URL_API
import net.kyori.adventure.webui.URL_EDITOR
import net.kyori.adventure.webui.URL_EDITOR_INPUT
import net.kyori.adventure.webui.URL_EDITOR_OUTPUT
import net.kyori.adventure.webui.URL_MINI_TO_HTML
import net.kyori.adventure.webui.URL_MINI_TO_JSON
import net.kyori.adventure.webui.URL_MINI_TO_TREE
import net.kyori.adventure.webui.editor.EditorInput
import net.kyori.adventure.webui.tryDecodeFromString
import net.kyori.adventure.webui.websocket.Call
import net.kyori.adventure.webui.websocket.Packet
import net.kyori.adventure.webui.websocket.Placeholders
import net.kyori.adventure.webui.websocket.Response
import org.w3c.dom.BeforeUnloadEvent
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLPreElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.WebSocket
import org.w3c.dom.Window
import org.w3c.dom.asList
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.EventTarget
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.Headers
import org.w3c.fetch.NO_CACHE
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestInit
import kotlin.js.Promise
import kotlin.js.json

private val homeUrl: String by lazy { window.location.href.split('?')[0] }
private val urlParams: URLSearchParams by lazy { URLSearchParams(window.location.search) }

private const val PARAM_INPUT: String = "input"
private const val PARAM_MODE: String = "mode"
public const val PARAM_BACKGROUND: String = "bg"
private const val PARAM_STRING_PLACEHOLDERS: String = "st"
private const val PARAM_COMPONENT_PLACEHOLDERS: String = "ct"

private var isInEditorMode: Boolean = false
private lateinit var editorInput: EditorInput

public lateinit var currentMode: Mode
private lateinit var webSocket: WebSocket

public fun main() {
    document.addEventListener(
        "DOMContentLoaded",
        {
            // Defaults for all toast messages
            bulmaToast.setDefaults(
                json(
                    "position" to "bottom-right",
                    "dismissible" to true,
                    "pauseOnHover" to true,
                    "duration" to 6000,
                    "animate" to json("in" to "fadeIn", "out" to "fadeOut")
                )
            )

            // EDITOR
            val input = document.getElementById("input")!!.unsafeCast<HTMLTextAreaElement>()
            val saveButton = document.getElementById("editor-save")!!
            urlParams.get(PARAM_EDITOR_TOKEN)?.let { token ->
                isInEditorMode = true

                window.fetch(
                    "$URL_API$URL_EDITOR$URL_EDITOR_INPUT?$PARAM_EDITOR_TOKEN=$token",
                    RequestInit("GET")
                ).then { response ->
                    if (!response.ok) {
                        isInEditorMode = false
                        bulmaToast.toast(
                            "Could not load editor session!",
                            type = "is-error"
                        )
                    } else {
                        response.text().then { text ->
                            val possibleEditorInput =
                                Serializers.json.tryDecodeFromString<EditorInput>(text)
                            if (possibleEditorInput == null) {
                                isInEditorMode = false
                                bulmaToast.toast(
                                    "Could not load editor session!",
                                    type = "is-error"
                                )
                            } else {
                                isInEditorMode = true
                                editorInput = possibleEditorInput
                                input.value = editorInput.input
                                bulmaToast.toast(
                                    "Loaded editor session! Press the save icon to generate a command to save the message to ${editorInput.application}."
                                )
                                saveButton.classList.remove("is-hidden")
                            }
                        }
                    }
                }
            }
            saveButton.addEventListener(
                "click",
                {
                    if (isInEditorMode && ::editorInput.isInitialized) {
                        window.fetch(
                            "$URL_API$URL_EDITOR$URL_EDITOR_OUTPUT",
                            RequestInit("POST", body = input.value)
                        ).then { response ->
                            response.text().then { token ->
                                window.navigator.clipboard.writeText(
                                    editorInput.command.replace("{token}", token)
                                )
                                    .then {
                                        bulmaToast.toast(
                                            "The command to run in-game has been copied to your clipboard!"
                                        )
                                    }
                            }
                        }
                    }
                }
            )

            // WEBSOCKET
            webSocket =
                if (window.location.hostname == "localhost" ||
                    window.location.hostname == "127.0.0.1"
                ) {
                    WebSocket("ws://${window.location.host}$URL_API$URL_MINI_TO_HTML")
                } else {
                    WebSocket("wss://${window.location.host}$URL_API$URL_MINI_TO_HTML")
                }
            webSocket.onopen = { onWebsocketReady() }

            // CORRECT HOME LINK
            document.getElementById("home-link")!!.unsafeCast<HTMLAnchorElement>().href = homeUrl

            // OBFUSCATION
            window.setInterval({ obfuscateAll() }, 10)

            // OUTPUT BOXES
            val outputPre = document.getElementById("output-pre")!!.unsafeCast<HTMLPreElement>()
            val outputPane = document.getElementById("output-pane")!!.unsafeCast<HTMLDivElement>()

            // CARET
            val chatBox = document.getElementById("chat-entry-box")!!.unsafeCast<HTMLDivElement>()
            window.setInterval({ chatBox.innerHTML = if (chatBox.innerHTML == "_") " " else "_" }, 380)

            // BUTTONS
            val settingsBox = document.getElementById("settings-box")
            document.getElementsByClassName("settings-button").asList().forEach { element ->
                element.addEventListener("click", { settingsBox!!.classList.toggle("is-active") })
            }
            val placeholdersBox = document.getElementById("placeholders-box")
            document.getElementsByClassName("placeholders-button").asList().forEach { element ->
                element.addEventListener(
                    "click",
                    {
                        // classList.toggle returns whether the class is in the classlist after the operation
                        // Since we care about updating everything after the uses closes the modal, we must negate the result.
                        val opened = placeholdersBox!!.classList.toggle("is-active")
                        if (!opened) {
                            val newPlaceholders = readPlaceholders()
                            window.localStorage[PARAM_STRING_PLACEHOLDERS] = Serializers.json.encodeToString(
                                newPlaceholders.stringPlaceholders
                            )
                            window.localStorage[PARAM_COMPONENT_PLACEHOLDERS] = Serializers.json.encodeToString(
                                newPlaceholders.miniMessagePlaceholders
                            )
                            webSocket.send(newPlaceholders)
                        }
                    }
                )
            }
            document.getElementsByClassName("add-placeholder-button").asList().forEach { element ->
                element.addEventListener("click", { UserPlaceholder.addToList() })
            }

            // MODES
            val urlParams = URLSearchParams(window.location.search)
            currentMode = Mode.fromString(urlParams.getFromParamsOrLocalStorage(PARAM_MODE))
            outputPre.classList.add(currentMode.className)
            outputPane.classList.add(currentMode.className)

            val modeButtons = document.getElementsByClassName("mc-mode").asList().unsafeCast<List<HTMLElement>>()
            modeButtons.forEach { element ->
                // set is-active on the current mode first
                val mode = Mode.valueOf(element.dataset["mode"]!!)
                if (currentMode == mode) {
                    element.classList.add("is-active")
                }

                // then add event listeners for the rest
                element.addEventListener(
                    "click",
                    { event ->
                        // remove active
                        modeButtons.forEach { button -> button.classList.remove("is-active") }

                        // now add it again lmao 10/10 code
                        val button = event.target!!.unsafeCast<HTMLAnchorElement>()
                        button.classList.add("is-active")
                        currentMode = mode
                        // Store current mode for persistence
                        window.localStorage[PARAM_MODE] = currentMode.paramName

                        // swap the class for the pane
                        Mode.MODES.forEach { mode ->
                            if (currentMode == mode) {
                                outputPre.classList.add(mode.className)
                                outputPane.classList.add(mode.className)
                            } else {
                                outputPre.classList.remove(mode.className)
                                outputPane.classList.remove(mode.className)
                            }
                        }

                        updateBackground()

                        // re-parse to remove the horrible server list header line hack
                        parse()
                    }
                )
            }

            // SETTINGS
            val settingBackground = document.getElementById("setting-background")!!.unsafeCast<HTMLSelectElement>()
            currentBackground = urlParams.getFromParamsOrLocalStorage(PARAM_BACKGROUND) ?: settingBackground.value
            settingBackground.addEventListener(
                "change",
                {
                    currentBackground = settingBackground.value
                }
            )

            // CLIPBOARD
            document.getElementById("link-share-button")!!.addEventListener(
                "click",
                {
                    val inputValue = encodeURIComponent(input.value)
                    val placeholders = readPlaceholders()
                    var link =
                        "$homeUrl?$PARAM_MODE=${currentMode.paramName}&$PARAM_INPUT=$inputValue"
                    if (currentMode != Mode.SERVER_LIST) {
                        link += "&$PARAM_BACKGROUND=$currentBackground"
                    }
                    if (!placeholders.stringPlaceholders.isNullOrEmpty()) {
                        link += "&$PARAM_STRING_PLACEHOLDERS="
                        link +=
                            encodeURIComponent(
                                Serializers.json.encodeToString(placeholders.stringPlaceholders)
                            )
                    }
                    if (!placeholders.miniMessagePlaceholders.isNullOrEmpty()) {
                        link += "&$PARAM_COMPONENT_PLACEHOLDERS="
                        link +=
                            encodeURIComponent(
                                Serializers.json.encodeToString(
                                    placeholders.miniMessagePlaceholders
                                )
                            )
                    }
                    window.navigator.clipboard.writeText(link).then {
                        bulmaToast.toast("Shareable link copied to clipboard!")
                    }
                }
            )
            document.getElementById("copy-button")!!.addEventListener(
                "click",
                {
                    window.navigator.clipboard.writeText(input.value.replace("\n", "\\n")).then {
                        bulmaToast.toast("Input text copied to clipboard!")
                    }
                }
            )
            document.getElementById("export-to-json-button")!!.addEventListener(
                "click",
                {
                    window.postPacket(
                        "$URL_API$URL_MINI_TO_JSON",
                        Call(miniMessage = input.value)
                    )
                        .then { response ->
                            response.text().then { text ->
                                window.navigator.clipboard.writeText(text).then {
                                    bulmaToast.toast("JSON copied to clipboard!")
                                }
                            }
                        }
                }
            )

            document.getElementById("show-tree-button")!!.addEventListener(
                "click",
                {
                    window.postPacket(
                        "$URL_API$URL_MINI_TO_TREE",
                        Call(miniMessage = input.value)
                    ).then { response ->
                        response.text().then { text ->
                            val escaped =
                                text.replace("&", "&amp;")
                                    .replace("<", "&lt;")
                                    .replace(">", "&gt;")
                            bulmaToast.toast("<pre>$escaped</pre>")
                        }
                    }
                }
            )

            // EDITOR

            // BURGER MENU
            val burgerMenu = document.getElementById("burger-menu")!!
            val navbarMenu = document.getElementById("navbar-menu")!!
            burgerMenu.addEventListener(
                "click",
                {
                    burgerMenu.classList.toggle("is-active")
                    navbarMenu.classList.toggle("is-active")
                }
            )

            // EVENTS
            document.addEventListener(
                "click",
                { event ->
                    val target = event.target
                    if (target is HTMLSpanElement && target.classList.contains(COMPONENT_CLASS)) {
                        checkClickEvents(target, EventType.all())

                        // we need to prevent propagation as we do that ourselves manually
                        event.stopPropagation()
                    }
                }
            )

            installHoverManager()
            installStyleButtons()
        }
    )

    window.addEventListener(
        "beforeunload",
        { event ->
            val input = document.getElementById("input")!!.unsafeCast<HTMLTextAreaElement>()
            if (input.value != "") {
                val e = event as BeforeUnloadEvent
                e.preventDefault()
                e.returnValue = "" // Chrome
            }
        }
    )
}

private fun readPlaceholders(): Placeholders {
    val userPlaceholders = UserPlaceholder.allInList()
    val stringPlaceholders = mutableMapOf<String, String>()
    val miniMessagePlaceholders = mutableMapOf<String, String>()
    userPlaceholders.filter { t -> t.key.isNotEmpty() && t.value.isNotEmpty() }.forEach { t ->
        (if (t.isMiniMessage) miniMessagePlaceholders else stringPlaceholders)[t.key] = t.value
    }
    return Placeholders(
        stringPlaceholders = stringPlaceholders, miniMessagePlaceholders = miniMessagePlaceholders
    )
}

private fun onWebsocketReady() {
    // SHARING
    val inputBox = document.getElementById("input")!!.unsafeCast<HTMLTextAreaElement>()

    if (!isInEditorMode) {
        urlParams.getFromParamsOrLocalStorage(PARAM_INPUT)?.also { inputString ->
            inputBox.value = inputString
        }
        var stringPlaceholders: Map<String, String>? = null
        var miniMessagePlaceholders: Map<String, String>? = null
        urlParams.getFromParamsOrLocalStorage(PARAM_STRING_PLACEHOLDERS)?.also { inputString ->
            stringPlaceholders = Serializers.json.tryDecodeFromString(inputString)
        }
        urlParams.getFromParamsOrLocalStorage(PARAM_COMPONENT_PLACEHOLDERS)?.also { inputString ->
            miniMessagePlaceholders = Serializers.json.tryDecodeFromString(inputString)
        }
        stringPlaceholders?.forEach { (k, v) ->
            UserPlaceholder.addToList().apply {
                key = k
                value = v
            }
        }
        miniMessagePlaceholders?.forEach { (k, v) ->
            UserPlaceholder.addToList().apply {
                isMiniMessage = true
                key = k
                value = v
            }
        }
        webSocket.send(
            Placeholders(
                stringPlaceholders = stringPlaceholders,
                miniMessagePlaceholders = miniMessagePlaceholders
            )
        )
    }

    parse()

    // INPUT
    val input = document.getElementById("input")!!.unsafeCast<HTMLTextAreaElement>()
    input.addEventListener("keyup", { parse() })
    input.addEventListener("change", { parse() })
    input.addEventListener(
        "paste",
        { event ->
            event.preventDefault()
            val paste = event.unsafeCast<ClipboardEvent>().clipboardData!!.getData("text")
            document.execCommand("insertText", false, paste.replace("\\n", "\n"))
        }
    )
    val output = document.getElementById("output-pre")!!
    webSocket.onmessage =
        { messageEvent ->
            val data = messageEvent.data
            if (data is String) {
                val response = Serializers.json.tryDecodeFromString<Response>(data)

                response?.parseResult?.let { result ->
                    if (result.success && result.dom != null) {
                        output.textContent = ""

                        result.dom.split("\n").forEach { line ->
                            if (line.isNotEmpty()) {
                                document.createElement("div").also { div ->
                                    div.innerHTML = line
                                    output.append(div)
                                }
                            }
                        }

                        // reset scroll to bottom (like how chat works)
                        if (currentMode == Mode.CHAT_OPEN || currentMode == Mode.CHAT_CLOSED) {
                            output.scrollTop = output.scrollHeight.toDouble()
                        }
                    } else if (!result.success && result.errorMessage != null) {
                        console.error("A parse error occurred: ${result.errorMessage}")
                    } else {
                        console.error("An unknown error occurred!")
                    }
                }
            }
        }
}

private fun checkClickEvents(target: EventTarget?, typesToCheck: Collection<EventType>) {
    if (target is HTMLSpanElement && target.classList.contains(COMPONENT_CLASS)) {
        val remainingTypesToCheck = mutableSetOf<EventType>()

        if (EventType.CLICK.isUsable(currentMode) && typesToCheck.contains(EventType.CLICK)) {
            val clickAction = target.dataset[DATA_CLICK_EVENT_ACTION.camel]

            if (clickAction == null) {
                remainingTypesToCheck.add(EventType.CLICK)
            } else {
                val content = target.dataset[DATA_CLICK_EVENT_VALUE.camel] ?: ""
                val actionName = clickAction.replace('_', ' ').replaceFirstChar(Char::uppercase)
                bulmaToast.toast(
                    "<p><b>Click Event</b></p><p>Action: <i>$actionName</i></p><p>Content: <i>$content</i></p>",
                    type = "is-info"
                )
            }
        }

        if (EventType.INSERTION.isUsable(currentMode) &&
            typesToCheck.contains(EventType.INSERTION)
        ) {
            val insertion = target.dataset[DATA_INSERTION.camel]

            if (insertion == null) {
                typesToCheck + EventType.INSERTION
            } else {
                bulmaToast.toast(
                    "<p><b>Insertion</b></p><p>Content: <i>$insertion</i></p>",
                    type = "is-info"
                )
            }
        }

        if (remainingTypesToCheck.isNotEmpty()) {
            // recurse up to the parent
            checkClickEvents(target.parentElement, remainingTypesToCheck)
        }
    }
}

private fun obfuscateAll() {
    document.getElementsByClassName("obfuscated").asList().forEach { obfuscate(it) }
}

private fun obfuscate(input: Element) {
    if (input.hasClass("hover")) return
    if (input.childElementCount > 0) {
        input.children.asList().forEach { obfuscate(it) }
    } else if (input.textContent != null) {
        input.textContent = obfuscate(input.textContent!!)
    }
}

private fun CharArray.map(transform: (Char) -> Char): CharArray {
    for (i in this.indices) {
        this[i] = transform(this[i])
    }
    return this
}

private fun obfuscate(input: String): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

    return input.toCharArray().map { if (it != ' ') allowedChars.random() else it }.concatToString()
}

@OptIn(ExperimentalStdlibApi::class)
private fun parse() {
    // don't do anything if we're not initialised yet
    if (::webSocket.isInitialized) {
        val input = document.getElementById("input")!!.unsafeCast<HTMLTextAreaElement>().value
        // Store current input for persistence
        window.localStorage[PARAM_INPUT] = input

        if (input.isEmpty() && currentMode != Mode.SERVER_LIST) {
            // we don't want to parse if input is empty (server list mode is an exception!)
            document.getElementById("output-pre")!!.textContent = ""
        } else {
            val lines =
                input.split("\n", "\\n").let { list ->
                    // some modes can only render a certain amount of lines
                    when (currentMode) {
                        Mode.CHAT_CLOSED -> list.safeSubList(0, 10)
                        Mode.SERVER_LIST ->
                            buildList(3) {
                                add(
                                    "KyoriCraft                                                 <gray>0<dark_gray>/</dark_gray>20"
                                )
                                add(list.getOrNull(0) ?: "\u200B")
                                add(list.getOrNull(1) ?: "\u200B")
                            }
                        else -> list
                    }
                }

            val combinedLines =
                lines.joinToString(separator = "\n") { line ->
                    // we don't want to lose empty lines, so replace them with zero-width space
                    if (line == "") "\u200B" else line
                }

            webSocket.send(Call(combinedLines))
        }
    }
}

private inline fun <reified T> List<T>.safeSubList(startIndex: Int, endIndex: Int): List<T> =
    if (endIndex > size) this else this.subList(startIndex, endIndex)

private fun WebSocket.send(packet: Packet) {
    this.send(Serializers.json.encodeToString(packet))
}

private inline fun <reified T : Packet> Window.postPacket(url: String, packet: T): Promise<org.w3c.fetch.Response> {
    return this.fetch(
        url,
        RequestInit(
            method = "POST",
            cache = RequestCache.NO_CACHE,
            headers = Headers(json("Content-Type" to "text/plain; charset=UTF-8")),
            body = Serializers.json.encodeToString(packet)
        )
    )
}
