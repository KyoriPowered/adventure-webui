package net.kyori.adventure.webui.js

import kotlin.js.json
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.hasClass
import net.kyori.adventure.webui.COMPONENT_CLASS
import net.kyori.adventure.webui.DATA_CLICK_EVENT_ACTION
import net.kyori.adventure.webui.DATA_CLICK_EVENT_VALUE
import net.kyori.adventure.webui.DATA_INSERTION
import net.kyori.adventure.webui.URL_API
import net.kyori.adventure.webui.URL_MINI_TO_HTML
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLPreElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.asList
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.EventTarget
import org.w3c.dom.get
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.NO_CACHE
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestInit

private val homeUrl: String by lazy { window.location.href.split('?')[0] }
private lateinit var currentMode: Mode

private const val PARAM_INPUT: String = "input"
private const val PARAM_MODE: String = "mode"

private var needsUpdate: Boolean = false

public fun main() {
    document.addEventListener(
        "DOMContentLoaded",
        {
            // CORRECT HOME LINK
            document.getElementById("home-link")!!.unsafeCast<HTMLAnchorElement>().href = homeUrl

            // SHARING
            val inputBox = document.getElementById("input")!!.unsafeCast<HTMLTextAreaElement>()
            val urlParams = URLSearchParams(window.location.search)
            val outputPre = document.getElementById("output-pre")!!.unsafeCast<HTMLPreElement>()
            val outputPane = document.getElementById("output-pane")!!.unsafeCast<HTMLDivElement>()

            currentMode = Mode.fromString(urlParams.get(PARAM_MODE))
            outputPre.classList.add(currentMode.className)
            outputPane.classList.add(currentMode.className)

            urlParams.get(PARAM_INPUT)?.also { inputString ->
                val text = decodeURIComponent(inputString)
                inputBox.value = text
                needsUpdate = true
                println("SHARED: $text")
                parse()
            }

            // INPUT
            val input = document.getElementById("input")!!.unsafeCast<HTMLTextAreaElement>()
            input.addEventListener("keyup", { needsUpdate = true })
            input.addEventListener("change", { needsUpdate = true })
            input.addEventListener(
                "paste",
                { event ->
                    event.preventDefault()
                    val paste = event.unsafeCast<ClipboardEvent>().clipboardData!!.getData("text")
                    document.execCommand("insertText", false, paste.replace("\\n", "\n"))
                })
            window.setInterval({ parse() }, 50)

            // OBFUSCATION
            window.setInterval({ obfuscateAll() }, 10)

            // CARET
            val chatBox = document.getElementById("chat-entry-box")!!.unsafeCast<HTMLDivElement>()
            window.setInterval(
                { chatBox.innerHTML = if (chatBox.innerHTML == "_") " " else "_" }, 380)

            // BUTTONS
            val settingsBox = document.getElementById("settings-box")
            document.getElementsByClassName("settings-button").asList().forEach { element ->
                element.addEventListener("click", { settingsBox!!.classList.toggle("is-active") })
            }

            val modeButtons =
                document.getElementsByClassName("mc-mode").asList().unsafeCast<List<HTMLElement>>()
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
                    })
            }

            // CLIPBOARD
            document.getElementById("link-share-button")!!.addEventListener(
                "click",
                {
                    window.navigator.clipboard.writeText(
                            "$homeUrl?$PARAM_MODE=${currentMode.paramName}&$PARAM_INPUT=${encodeURIComponent(input.value)}")
                        .then {
                            bulmaToast.toast(
                                json(
                                    "message" to "Shareable link copied to clipboard!",
                                    "type" to "is-success",
                                    "position" to "bottom-right",
                                    "dismissible" to true,
                                    "pauseOnHover" to true,
                                    "animate" to json("in" to "fadeIn", "out" to "fadeOut")))
                        }
                })
            document.getElementById("copy-button")!!.addEventListener(
                "click",
                {
                    window.navigator.clipboard.writeText(input.value.replace("\n", "\\n")).then {
                        bulmaToast.toast(
                            json(
                                "message" to "Input text copied to clipboard!",
                                "type" to "is-success",
                                "position" to "bottom-right",
                                "dismissible" to true,
                                "pauseOnHover" to true,
                                "animate" to json("in" to "fadeIn", "out" to "fadeOut")))
                    }
                })

            // BURGER MENU
            val burgerMenu = document.getElementById("burger-menu")!!
            val navbarMenu = document.getElementById("navbar-menu")!!
            burgerMenu.addEventListener(
                "click",
                {
                    burgerMenu.classList.toggle("is-active")
                    navbarMenu.classList.toggle("is-active")
                })

            // SETTINGS
            val settingBackground =
                document.getElementById("setting-background")!!.unsafeCast<HTMLSelectElement>()
            settingBackground.addEventListener(
                "change",
                {
                    outputPane.style.backgroundImage = "url(\"img/${settingBackground.value}.png\")"
                })

            // EVENTS
            document.addEventListener(
                "click",
                { event ->
                    val target = event.target
                    if (target is HTMLSpanElement && target.classList.contains(COMPONENT_CLASS)) {
                        checkEvents(target, EventType.all())

                        // we need to prevent propagation as we do that ourselves manually
                        event.stopPropagation()
                    }
                })
        })
}

private fun checkEvents(target: EventTarget?, typesToCheck: Collection<EventType>) {
    if (target is HTMLSpanElement && target.classList.contains(COMPONENT_CLASS)) {
        val remainingTypesToCheck = mutableSetOf<EventType>()

        if (EventType.CLICK.isUsable(currentMode) && typesToCheck.contains(EventType.CLICK)) {
            val clickAction = target.dataset[DATA_CLICK_EVENT_ACTION.camel]

            if (clickAction == null) {
                remainingTypesToCheck.add(EventType.CLICK)
            } else {
                val content = target.dataset[DATA_CLICK_EVENT_VALUE.camel] ?: ""
                bulmaToast.toast(
                    json(
                        "message" to
                            "<p><b>Click Event</b></p><p>Action: <i>${
                                    clickAction.replace('_', ' ').replaceFirstChar(Char::uppercase)
                                }</i></p><p>Content: <i>$content</i></p>",
                        "type" to "is-info",
                        "position" to "bottom-right",
                        "dismissible" to true,
                        "pauseOnHover" to true,
                        "animate" to json("in" to "fadeIn", "out" to "fadeOut")))
            }
        }

        if (EventType.HOVER.isUsable(currentMode) && typesToCheck.contains(EventType.HOVER)) {}

        if (EventType.INSERTION.isUsable(currentMode) &&
            typesToCheck.contains(EventType.INSERTION)) {
            val insertion = target.dataset[DATA_INSERTION.camel]

            if (insertion == null) {
                typesToCheck + EventType.INSERTION
            } else {
                bulmaToast.toast(
                    json(
                        "message" to "<p><b>Insertion</b></p><p>Content: <i>$insertion</i></p>",
                        "type" to "is-info",
                        "position" to "bottom-right",
                        "dismissible" to true,
                        "pauseOnHover" to true,
                        "animate" to json("in" to "fadeIn", "out" to "fadeOut")))
            }
        }

        if (remainingTypesToCheck.isNotEmpty()) {
            // recurse up to the parent
            checkEvents(target.parentElement, remainingTypesToCheck)
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

private fun parse() {
    if (needsUpdate) {
        val input = document.getElementById("input")!!.unsafeCast<HTMLTextAreaElement>()
        val output = document.getElementById("output-pre")!!
        val lines = input.value.split("\n")
        val combinedLines =
            lines.joinToString(separator = "\n") { line ->
                // we don't want to lose empty lines, so replace them with zero-width space
                if (line == "") "\u200B" else line
            }

        window.fetch(
                "$URL_API$URL_MINI_TO_HTML",
                RequestInit(
                    method = "POST",
                    cache = RequestCache.NO_CACHE,
                    headers = mapOf(Pair("Content-Type", "text/plain")),
                    body = combinedLines))
            .then { response ->
                response.text().then { content ->
                    output.textContent = ""

                    content.split("\n").forEachIndexed { index, line ->
                        // skip blank lines
                        if (line.isNotBlank()) {
                            val div = document.createElement("div")

                            if (lines[index] == "") div.classList.add("no-padding")

                            div.innerHTML = line
                            println("")
                            output.append(div)
                        }
                    }

                    // reset scroll to bottom (like how chat works)
                    if (currentMode == Mode.CHAT_OPEN || currentMode == Mode.CHAT_CLOSED) {
                        output.scrollTop = output.scrollHeight.toDouble()
                    }

                    needsUpdate = false
                }
            }
    }
}
