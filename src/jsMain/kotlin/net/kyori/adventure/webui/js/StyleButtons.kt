package net.kyori.adventure.webui.js

import kotlinx.browser.document
import org.w3c.dom.CustomEvent
import org.w3c.dom.CustomEventInit
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLTextAreaElement

public data class StyleTag(val beforeCursor: String, val placeholder: String, val beforeSel: String, val afterSel: String) {
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
    STYLE_WRAPPERS.forEach { (buttonName, tag) ->
        val inputBox = document.getElementById("input")!!.unsafeCast<HTMLTextAreaElement>()
        val button = document.getElementById("editor-$buttonName-button")!!.unsafeCast<HTMLButtonElement>()
        button.addEventListener(
            "click",
            {
                handleStyleButton(inputBox, tag)
            }
        )
    }
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
