package net.kyori.adventure.webui.js

import kotlinx.browser.document
import kotlinx.browser.window
import net.kyori.adventure.webui.COMPONENT_CLASS
import net.kyori.adventure.webui.DATA_HOVER_EVENT_SHOW_TEXT
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get

public fun installHoverManager() {
    val hoverTooltip = document.getElementById("hover-tooltip").unsafeCast<HTMLDivElement>()

    document.addEventListener(
        "mouseover",
        { event ->
            val target = event.target
            checkHoverEvents(target, hoverTooltip)
            // we need to prevent propagation as we do that ourselves manually
            event.stopPropagation()
        }
    )

    document.addEventListener(
        "mouseout",
        { event ->
            val target = event.target
            if (target is HTMLSpanElement && target.classList.contains(COMPONENT_CLASS)) {
                if (!hoverTooltip.hidden) {
                    hoverTooltip.hidden = true
                    hoverTooltip.innerHTML = ""
                }
                event.stopPropagation()
            }
        }
    )

    document.addEventListener(
        "mousemove",
        { event ->
            val e = event as MouseEvent
            var top = e.clientY - 34
            var left = e.clientX + 14
            val hoverWidth = hoverTooltip.clientWidth
            val hoverHeight = hoverTooltip.clientHeight
            val windowWidth = window.innerWidth
            val windowHeight = window.innerHeight

            // If going off the right of the screen, go to the left of the cursor
            if (left + hoverWidth > windowWidth) {
                left -= hoverWidth + 36
            }

            // If now going off to the left of the screen, resort to going above the cursor
            if (left < 0) {
                left = 0
                top -= hoverHeight - 22

                // Go below the cursor if too high
                if (top < 0) {
                    top += hoverHeight + 47
                }
            } else if (top < 0) {
                // Don't go off the top of the screen
                top = 0
            } else if (top + hoverHeight > windowHeight) {
                // Don't go off the bottom of the screen
                top = windowHeight - hoverHeight
            }

            hoverTooltip.style.top = "${top}px"
            hoverTooltip.style.left = "${left}px"
        }
    )
}

private fun checkHoverEvents(target: EventTarget?, hoverTooltip: HTMLDivElement) {
    if (target is HTMLSpanElement && target.classList.contains(COMPONENT_CLASS)) {
        if (EventType.HOVER.isUsable(currentMode)) {
            val showTextHover = target.dataset[DATA_HOVER_EVENT_SHOW_TEXT.camel]
            if (showTextHover != null) {
                hoverTooltip.hidden = false
                hoverTooltip.innerHTML = showTextHover
                // No further bubbling required
                return
            }
        }
        checkHoverEvents(target.parentElement, hoverTooltip)
    }
}
