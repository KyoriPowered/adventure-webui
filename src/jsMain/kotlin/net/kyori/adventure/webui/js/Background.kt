package net.kyori.adventure.webui.js

import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.asList
import org.w3c.dom.set

private val outputPane: HTMLDivElement by lazyDocumentElement("output-pane")
private val settingBackground: HTMLSelectElement by lazyDocumentElement("setting-background")
private val validBackgrounds: List<String> by lazy { settingBackground.options.asList().map { it.unsafeCast<HTMLOptionElement>().value } }
public var currentBackground: String? = null
    set(value) {
        field = value
        updateBackground()
    }

public fun updateBackground() {
    if (currentMode == Mode.SERVER_LIST) {
        // Remove the current background if we are switching to "server list"
        // as it has a black background that is otherwise overridden
        outputPane.style.removeProperty("background-image")
    } else {
        // Otherwise, try to put back the background from before
        val bg = currentBackground ?: return
        if (!validBackgrounds.contains(currentBackground)) return
        window.localStorage[PARAM_BACKGROUND] = bg
        settingBackground.value = bg
        outputPane.style.backgroundImage = "url(\"img/$bg.jpg\")"
    }
}
