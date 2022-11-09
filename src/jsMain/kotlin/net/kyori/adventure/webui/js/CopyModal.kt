package net.kyori.adventure.webui.js

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLPreElement
import org.w3c.dom.asList

private val modal: HTMLDivElement by lazyDocumentElement("copy-modal")
private val modalTitle: HTMLParagraphElement by lazyDocumentElement("copy-modal-title")
private val modalBody: HTMLPreElement by lazyDocumentElement("copy-modal-body")
private val modalButton: HTMLAnchorElement by lazyDocumentElement("copy-modal-button")
private val modalClose: List<Element> by lazy { document.getElementsByClassName("close-copy-modal").asList() }
private var eventListenerSet = false

// TODO(rymiel): Use this in more places where copying doesn't work i.e. the editor API safe button
public fun createCopyModal(title: String, body: String) {
    modal.classList.add("is-active")
    modalTitle.innerText = title
    modalBody.innerText = body

    if (!eventListenerSet) {
        eventListenerSet = true

        modalButton.addEventListener(
            "click",
            {
                val text = modalBody.innerText
                window.navigator.clipboard.writeText(text).catch { error ->
                    console.log(error) // Give up on trying to copy the thing
                }
            }
        )

        modalClose.forEach { element ->
            element.addEventListener(
                "click",
                {
                    modal.classList.remove("is-active")
                }
            )
        }
    }
}
