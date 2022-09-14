package net.kyori.adventure.webui.js

import kotlinx.browser.document
import org.w3c.dom.CustomEvent
import org.w3c.dom.CustomEventInit
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.asList
import org.w3c.dom.get

// https://iro.js.org/colorPicker_api.html
private external interface ColorPicker {
    // https://iro.js.org/guide.html#color-picker-events
    fun <T> on(event: String, callback: (T) -> Unit)
    var color: Color
}

// https://iro.js.org/color_api.html
private external interface Color {
    var hexString: String
}

// Copied out of adventure; can't use adventure api directly because this is javascript
private val NAMED_COLORS = mapOf(
    "#000000" to "black",
    "#0000aa" to "dark_blue",
    "#00aa00" to "dark_green",
    "#00aaaa" to "dark_aqua",
    "#aa0000" to "dark_red",
    "#aa00aa" to "dark_purple",
    "#ffaa00" to "gold",
    "#aaaaaa" to "gray",
    "#555555" to "dark_gray",
    "#5555ff" to "blue",
    "#55ff55" to "green",
    "#55ffff" to "aqua",
    "#ff5555" to "red",
    "#ff55ff" to "light_purple",
    "#ffff55" to "yellow",
    "#ffffff" to "white",
)

public data class StyleTag(val beforeCursor: String, val placeholder: String, val beforeSel: String, val afterSel: String) {
    public constructor(opening: String, closing: String) : this("", "", opening, closing)
    public constructor(simple: String) : this("", "", "<$simple>", "</$simple>")
}

public val STYLE_WRAPPERS: Map<String, StyleTag> = mapOf(
    "bold" to StyleTag("b"),
    "italic" to StyleTag("i"),
    "underline" to StyleTag("u"),
    "strikethrough" to StyleTag("st"),
    "open-url" to StyleTag("<click:open_url:\'", "url", "\'>", "</click>"),
    "run-command" to StyleTag("<click:run_command:\'", "/command", "\'>", "</click>"),
    "suggest-command" to StyleTag("<click:suggest_command:\'", "/command", "\'>", "</click>"),
    "hover-text" to StyleTag("<hover:show_text:\'", "text", "\'>", "</hover>"),
)

public fun installStyleButtons() {
    val inputBox = document.element<HTMLTextAreaElement>("input")

    STYLE_WRAPPERS.forEach { (buttonName, tag) ->
        val button = document.element<HTMLButtonElement>("editor-$buttonName-button")
        button.addEventListener(
            "click",
            {
                handleStyleButton(inputBox, tag)
            }
        )
    }

    val previewSwatch = document.element<HTMLDivElement>("preview-swatch")
    val previewHex = document.element<HTMLInputElement>("preview-hex")

    // I've given up on kotlin type safety
    val colorPicker = js("new iro.ColorPicker('#picker')").unsafeCast<ColorPicker>()
    colorPicker.on<Color>("color:change") { c ->
        previewSwatch.style.backgroundColor = c.hexString
        previewHex.value = c.hexString
        previewHex.classList.remove("is-danger")
    }

    previewHex.addEventListener(
        "input",
        {
            var validInput = false
            val newHex = previewHex.value
            // If the input isn't of the expected length for a full hex color, we assume the user is still typing it.
            // While iro.js can accept shorthand notations, adventure doesn't, so rejecting them here is probably fine.
            if (newHex.length == 7) {
                try {
                    // iro.js will let us know if the hex input is invalid by throwing an error.
                    colorPicker.color.hexString = newHex
                    // If we make it here, no error was thrown, and we can assume the input was a valid color!
                    validInput = true
                } catch (e: Throwable) {
                    // iro.js throws a generic Error instance, so this is as good of a check as you can do
                    if (e.message != "Invalid hex string") throw e
                }
            }

            // If the input wasn't valid, we add a subtle red outline to the input box
            if (validInput) previewHex.classList.remove("is-danger")
            else previewHex.classList.add("is-danger")
        }
    )

    val namedSwatch = document.element<HTMLDivElement>("named-swatch")
    namedSwatch.children.asList().forEach { swatch ->
        val swatchElement = swatch.unsafeCast<HTMLDivElement>()
        swatchElement.addEventListener("click", {
            colorPicker.color.hexString = swatchElement.dataset["color"].orEmpty()
            // Clicking (on a swatch) takes away focus from the editor, put it back so the user still sees
            // their selection and knows what they're about to modify.
            inputBox.focus()
        })
    }

    val useColorButton = document.element<HTMLButtonElement>("use-color")
    useColorButton.addEventListener(
        "click",
        {
            // Try to find a matching "named" color, which is probably more readable than a hex color
            val namedColor = NAMED_COLORS[previewHex.value]
            if (namedColor != null) {
                handleStyleButton(inputBox, StyleTag(namedColor))
            } else {
                handleStyleButton(inputBox, StyleTag("<color:${previewHex.value}>", "</color>"))
            }
            useColorButton.closest(".dropdown")!!.classList.toggle("is-active") // Roll up the dropdown after a color was applied
        }
    )
}

// This is a separate function just so the level of indentation isn't horrible for the comments...
private fun handleStyleButton(inputBox: HTMLTextAreaElement, tag: StyleTag) {
    val selStart = inputBox.selectionStart ?: 0
    val selEnd = inputBox.selectionEnd ?: 0
    var newSelStart = selStart
    var newSelEnd = selEnd

    val before = inputBox.value.substring(0, selStart)
    val selection = inputBox.value.substring(selStart, selEnd)
    val after = inputBox.value.substring(selEnd)
    val tagPrefix = tag.beforeCursor + tag.beforeSel
    val tagSuffix = tag.afterSel

    // These checks and modifications handle selection start and selection end separately. However, thanks to a cosmic
    // coincidence of math, they generalize perfectly to the case of having no text selected, that is, just having
    // your cursor somewhere in the text box (selection start equals selection end)

    if (before.endsWith(tagPrefix) && after.startsWith(tagSuffix)) {
        // This represents the case where the user's selection contains text which is immediately surrounded by the
        // relevant tags, so we remove those tags and leave the text selected as before.
        // Such as: text <b>|bold|</b> more text        (where | marks the edges of a selection)
        // This is very important for being able to "toggle off" a style right after selecting one.
        // We must "widen" the selection first so that the tags get replaced too with insertText
        inputBox.insertText(selection, tagPrefix.length, tagSuffix.length)
        newSelStart -= tagPrefix.length
        newSelEnd -= tagPrefix.length
    } else if (selection.startsWith(tagPrefix) && selection.endsWith(tagSuffix)) {
        // This represents the case where the user's selection perfectly encapsulates the relevant opening and closing
        // tag, so we remove that tag and leave only the text which was between said tags
        // Such as: text |<b>bold</b>| more text        (where | marks the edges of a selection)
        inputBox.insertText(selection.removeSurrounding(tagPrefix, tagSuffix))
        newSelEnd -= tagPrefix.length + tagSuffix.length
    } else {
        // If there don't seem to be already existing relevant tags near the selection, we add them!
        if (tag.beforeCursor.isBlank()) {
            // This is a simple surrounding tag such as <b></b>. We make sure to keep exactly the same area selected as
            // was selected before hitting the style button.
            inputBox.insertText(tagPrefix + selection + tagSuffix)
            newSelStart += tagPrefix.length
            newSelEnd += tagPrefix.length
        } else {
            // This is a tag which requires more user input, such as <input:open_url...>. For example, in the open_url
            // case, we helpfully position the cursor right where the url would go, so it can be typed right away.
            inputBox.insertText(tag.beforeCursor + tag.placeholder + tag.beforeSel + selection + tagSuffix)
            newSelStart += tag.beforeCursor.length
            newSelEnd = newSelStart + tag.placeholder.length
        }
    }

    // This makes sure the websocket stuff is actually fired so the preview stays in sync.
    inputBox.dispatchEvent(CustomEvent("change", CustomEventInit(bubbles = true, cancelable = true)))

    /*
     TODO(rymiel): This focus() is problematic on mobile, as it'll most likely throw up the keyboard all of a sudden
       which takes up a bunch of the screen. This isn't strictly a concern on the other focus() call above, as the
       color swatches are hidden on mobile anyway, but still something to probably disable when an on-screen keyboard
       is used
     */
    inputBox.focus()
    inputBox.setSelectionRange(newSelStart, newSelEnd)
}

/* Apparently execCommand is deprecated and non-standard and so is "insertText", however, it seems to be the only
 * real way to manipulate the text box content while also allowing for undoing without hijacking all the key events
 * or something. Being nonstandard, we should *probably* have a fallback which just sets the content (and thereby
 * nuking the undo-buffer, but oh well)
 * Also, not supported by Internet Explorer, but like, if you're using IE that's a 'you' problem.
 */
private fun HTMLTextAreaElement.insertText(text: String, widenSelStart: Int = 0, widenSelEnd: Int = 0) {
    this.focus()
    if (widenSelStart != 0 && widenSelEnd != 0) {
        this.setSelectionRange(
            (this.selectionStart ?: 0) - widenSelStart,
            (this.selectionEnd ?: 0) + widenSelEnd
        )
    }
    document.execCommand("insertText", false, text)
}
