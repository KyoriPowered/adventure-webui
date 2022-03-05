package net.kyori.adventure.webui.js

import kotlinx.browser.document
import kotlinx.html.dom.append
import kotlinx.html.js.a
import kotlinx.html.js.i
import kotlinx.html.js.input
import kotlinx.html.js.span
import kotlinx.html.js.td
import kotlinx.html.js.tr
import kotlinx.html.title
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLTableRowElement
import org.w3c.dom.asList

public class UserPlaceholder(
    private val keyInput: HTMLInputElement,
    private val valueInput: HTMLInputElement
) {
    public var key: String by keyInput::value
    public var value: String by valueInput::value

    public companion object {
        public fun addToList(): UserPlaceholder {
            val list = document.getElementById("placeholders-list")!!
            lateinit var key: HTMLInputElement
            lateinit var value: HTMLInputElement
            lateinit var row: HTMLTableRowElement
            lateinit var deleteButton: HTMLAnchorElement
            list.append {
                row = tr {
                    td(classes = "control") {
                        key = input(classes = "input placeholder-key") {
                            pattern = "[!?#]?[a-z0-9_-]*" // Can this be less of a magic value?
                        }
                    }
                    td(classes = "control") { value = input(classes = "input placeholder-value") }
                    td(classes = "control") {
                        deleteButton = a(classes = "button is-danger") {
                            title = "Remove placeholder"
                            attributes["aria-label"] = "Remove placeholder"
                            span(classes = "icon is-small") {
                                i(classes = "fas fa-trash-alt")
                            }
                        }
                    }
                }
            }
            key.addEventListener("input", {
                val activeKeys = document.getElementsByClassName("placeholder-key").asList().map { it as HTMLInputElement }
                val tip = document.getElementById("placeholder-tip") as HTMLParagraphElement
                if (activeKeys.all { it.checkValidity() }) {
                    tip.style.display = "none"
                } else {
                    tip.style.display = "block"
                }
            })
            deleteButton.addEventListener("click", { row.remove() })
            return UserPlaceholder(key, value)
        }

        public fun allInList(): List<UserPlaceholder> {
            val placeholdersBox = document.getElementById("placeholders-list")!!
            val placeholderKeys =
                placeholdersBox.getElementsByClassName("placeholder-key").asList().map {
                    it.unsafeCast<HTMLInputElement>()
                }
            val placeholderValues =
                placeholdersBox.getElementsByClassName("placeholder-value").asList().map {
                    it.unsafeCast<HTMLInputElement>()
                }
            return List(placeholderKeys.size) { i ->
                UserPlaceholder(placeholderKeys[i], placeholderValues[i])
            }
        }
    }
}
