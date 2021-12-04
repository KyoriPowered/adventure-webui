package net.kyori.adventure.webui.js

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.asList
import org.w3c.dom.set

private val outputPane: HTMLDivElement by lazy { document.getElementById("output-pane")!!.unsafeCast<HTMLDivElement>() }
private val settingBackground: HTMLSelectElement by lazy { document.getElementById("setting-background")!!.unsafeCast<HTMLSelectElement>() }
private val validBackgrounds: List<String> by lazy { settingBackground.options.asList().map { it.unsafeCast<HTMLOptionElement>().value } }
public var currentBackground: String? = null
    set(value) {
        field = value
        updateBackground()
    }

public fun updateBackground() {
    val bg = currentBackground ?: return
    if (!validBackgrounds.contains(currentBackground)) return
    window.localStorage[PARAM_BACKGROUND] = bg
    settingBackground.value = bg
    if (currentMode == Mode.SERVER_LIST) {
        // Remove the current background if we are switching to "server list"
        // as it has a black background that is otherwise overridden
        outputPane.style.removeProperty("background-image")
    } else {
        // Otherwise, try to put back the background from before
        outputPane.style.backgroundImage = "url(\"img/$bg.jpg\")"
    }
}
