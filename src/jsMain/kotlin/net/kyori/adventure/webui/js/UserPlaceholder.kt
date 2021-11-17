package net.kyori.adventure.webui.js

import kotlinx.browser.document
import kotlinx.html.InputType
import kotlinx.html.dom.append
import kotlinx.html.js.input
import kotlinx.html.js.td
import kotlinx.html.js.tr
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList

public class UserPlaceholder(
    private val isMiniMessageCheckbox: HTMLInputElement,
    private val keyInput: HTMLInputElement,
    private val valueInput: HTMLInputElement
) {
    public var isMiniMessage: Boolean
        get() = isMiniMessageCheckbox.checked
        set(value) {
            isMiniMessageCheckbox.checked = value
        }

    public var key: String
        get() = keyInput.value
        set(value) {
            keyInput.value = value
        }

    public var value: String
        get() = valueInput.value
        set(value) {
            valueInput.value = value
        }

    public companion object {
        public fun addToList(): UserPlaceholder {
            val list = document.getElementById("placeholders-list")!!
            lateinit var key: HTMLInputElement
            lateinit var value: HTMLInputElement
            lateinit var isMM: HTMLInputElement
            list.append {
                tr {
                    td(classes = "control is-vcentered has-text-centered") {
                        isMM = input(type = InputType.checkBox, classes = "placeholder-component")
                    }
                    td(classes = "control") { key = input(classes = "input placeholder-key") }
                    td(classes = "control") { value = input(classes = "input placeholder-value") }
                }
            }
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
