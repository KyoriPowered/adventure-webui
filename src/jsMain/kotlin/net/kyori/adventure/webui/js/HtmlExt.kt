package net.kyori.adventure.webui.js

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.NonElementParentNode

/**
 * Find an element with the given [id], asserting that it exists, and directly cast it to [T].
 *
 * WARNING: This method is inherently unsafe, as it will fail hard if an element with the specified id does not exist,
 *          or it isn't of the expected type!
 */
public fun <T : Element> NonElementParentNode.element(id: String): T {
    return getElementById(id)!!.unsafeCast<T>() // "Who needs type safety in Kotlin/JS anyway"
}

/**
 * A delegate factory for an element lazily fetched from the [document]. See [NonElementParentNode.element].
 */
public fun <T : Element> lazyDocumentElement(id: String): Lazy<T> {
    return lazy { document.element(id) }
}
