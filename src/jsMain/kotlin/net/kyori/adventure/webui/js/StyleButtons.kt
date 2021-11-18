package net.kyori.adventure.webui.js

import kotlinx.browser.document
import org.w3c.dom.CustomEvent
import org.w3c.dom.CustomEventInit
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLTextAreaElement

public val STYLE_WRAPPERS: Map<String, String> = mapOf(
    "bold" to "b",
    "italic" to "i",
    "underline" to "u",
    "strikethrough" to "st"
)

public fun installStyleButtons() {
    STYLE_WRAPPERS.forEach { (buttonName, tagName) ->
        val inputBox = document.getElementById("input")!!.unsafeCast<HTMLTextAreaElement>()
        val button = document.getElementById("editor-$buttonName-button")!!.unsafeCast<HTMLButtonElement>()
        button.addEventListener(
            "click",
            {
                val selStart = inputBox.selectionStart ?: 0
                val selEnd = inputBox.selectionEnd ?: 0
                val startAdjust: Int
                val endAdjust: Int

                val before = inputBox.value.substring(0, selStart)
                val selection = inputBox.value.substring(selStart, selEnd)
                val after = inputBox.value.substring(selEnd)

                val openingTag = "<$tagName>"
                val closingTag = "</$tagName>"

                if (before.endsWith(openingTag) && after.startsWith(closingTag)) {
                    inputBox.value = before.removeSuffix(openingTag) + selection + after.removePrefix(closingTag)
                    startAdjust = -openingTag.length
                    endAdjust = -openingTag.length
                } else if (selection.startsWith(openingTag) && selection.endsWith(closingTag)) {
                    inputBox.value = before + selection.removeSurrounding(openingTag, closingTag) + after
                    startAdjust = 0
                    endAdjust = -(openingTag.length + closingTag.length)
                } else {
                    inputBox.value = before + openingTag + selection + closingTag + after
                    startAdjust = openingTag.length
                    endAdjust = openingTag.length
                }

                inputBox.dispatchEvent(CustomEvent("change", CustomEventInit(bubbles = true, cancelable = true)))
                inputBox.focus()
                inputBox.setSelectionRange(selStart + startAdjust, selEnd + endAdjust)
            }
        )
    }
}
