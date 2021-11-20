package net.kyori.adventure.webui.js

import kotlinx.browser.document
import kotlinx.html.InputType
import kotlinx.html.classes
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
import org.w3c.dom.HTMLTableRowElement
import org.w3c.dom.asList

public class UserPlaceholder(
    private val isMiniMessageCheckbox: HTMLInputElement,
    private val keyInput: HTMLInputElement,
    private val valueInput: HTMLInputElement
) {
    public var isMiniMessage: Boolean by isMiniMessageCheckbox::checked
    public var key: String by keyInput::value
    public var value: String by valueInput::value

    public companion object {
        public fun addToList(): UserPlaceholder {
            val list = document.getElementById("placeholders-list")!!
            lateinit var key: HTMLInputElement
            lateinit var value: HTMLInputElement
            lateinit var isMM: HTMLInputElement
            lateinit var row: HTMLTableRowElement
            lateinit var deleteButton: HTMLAnchorElement
            list.append {
                row = tr {
                    td(classes = "control is-vcentered has-text-centered") {
                        isMM = input(type = InputType.checkBox, classes = "placeholder-component")
                    }
                    td(classes = "control") { key = input(classes = "input placeholder-key") }
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
            deleteButton.addEventListener("click", { row.remove() })
            return UserPlaceholder(isMM, key, value)
        }

        public fun allInList(): List<UserPlaceholder> {
            val placeholdersBox = document.getElementById("placeholders-list")!!
            val placeholderIsMM =
                placeholdersBox.getElementsByClassName("placeholder-component").asList().map {
                    it.unsafeCast<HTMLInputElement>()
                }
            val placeholderKeys =
                placeholdersBox.getElementsByClassName("placeholder-key").asList().map {
                    it.unsafeCast<HTMLInputElement>()
                }
            val placeholderValues =
                placeholdersBox.getElementsByClassName("placeholder-value").asList().map {
                    it.unsafeCast<HTMLInputElement>()
                }
            return placeholderIsMM.mapIndexed { i, _ ->
                UserPlaceholder(placeholderIsMM[i], placeholderKeys[i], placeholderValues[i])
            }
        }
    }
}
