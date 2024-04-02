package net.kyori.adventure.webui.jvm.minimessage.hook

import kotlinx.html.classes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.webui.COMPONENT_CLASS
import net.kyori.adventure.webui.DATA_CLICK_EVENT_ACTION
import net.kyori.adventure.webui.DATA_CLICK_EVENT_VALUE
import net.kyori.adventure.webui.DATA_HOVER_EVENT_SHOW_TEXT
import net.kyori.adventure.webui.DATA_INSERTION
import net.kyori.adventure.webui.jvm.addData
import net.kyori.adventure.webui.jvm.addStyle
import net.kyori.adventure.webui.jvm.appendComponent

/** A render hook for hover events. */
public val HOVER_EVENT_RENDER_HOOK: ComponentRenderHook = { component ->
    component.hoverEvent()?.let { hoverEvent ->
        if (hoverEvent.action() == HoverEvent.Action.SHOW_TEXT) {
            val hoverHtml = StringBuilder()
            hoverHtml.appendComponent(HookManager.render(hoverEvent.value() as Component))
            addData(DATA_HOVER_EVENT_SHOW_TEXT, hoverHtml.toString())
        }
        // unknown hover events are discarded FOR NOW
    }

    true
}

/** A render hook for click events. */
public val CLICK_EVENT_RENDER_HOOK: ComponentRenderHook = { component ->
    component.clickEvent()?.let { clickEvent ->
        addData(DATA_CLICK_EVENT_ACTION, clickEvent.action().toString())
        addData(DATA_CLICK_EVENT_VALUE, clickEvent.value())
    }

    true
}

/** A render hook for insertions. */
public val INSERTION_RENDER_HOOK: ComponentRenderHook = { component ->
    component.insertion()?.let { insertion -> addData(DATA_INSERTION, insertion) }

    true
}

/** A render hook that adds [COMPONENT_CLASS] to the span class list. */
public val COMPONENT_CLASS_RENDER_HOOK: ComponentRenderHook = { _ ->
    classes = classes + COMPONENT_CLASS

    true
}

/** A render hook for text coloring and shadow. */
public val TEXT_COLOR_RENDER_HOOK: ComponentRenderHook = { component ->
    component.color()?.let { color ->
        addStyle("color: ${color.asHexString()}")

        val r = color.red() / 4.0
        val g = color.green() / 4.0
        val b = color.blue() / 4.0
        addStyle("text-shadow: 3px 3px rgb($r, $g, $b)")
    }

    true
}

/** A render hook for text decoration. */
public val TEXT_DECORATION_RENDER_HOOK: ComponentRenderHook = { component ->
    TextDecoration.NAMES.values().forEach { decoration ->
        val state = component.decoration(decoration)

        when (decoration) {
            TextDecoration.OBFUSCATED ->
                if (state == TextDecoration.State.TRUE) classes = classes + "obfuscated"
            TextDecoration.BOLD ->
                if (state == TextDecoration.State.TRUE) addStyle("font-weight: bold")
                else if (state == TextDecoration.State.FALSE) addStyle("font-weight: normal")
            TextDecoration.ITALIC ->
                if (state == TextDecoration.State.TRUE) addStyle("font-style: italic")
                else if (state == TextDecoration.State.FALSE) addStyle("font-style: normal")
            else -> {} // do nothing, we handle the other states later
        }
    }

    val underline = component.decoration(TextDecoration.UNDERLINED)
    val strikethrough = component.decoration(TextDecoration.STRIKETHROUGH)
    var textDecoration = ""
    if (underline == TextDecoration.State.TRUE) textDecoration += "underline "
    if (strikethrough == TextDecoration.State.TRUE) textDecoration += "line-through"
    if (underline == TextDecoration.State.FALSE && strikethrough == TextDecoration.State.FALSE)
        textDecoration = "none"
    if (textDecoration != "") addStyle("text-decoration: ${textDecoration.trim()}")

    true
}

/** A render hook for fonts. */
public val FONT_RENDER_HOOK: ComponentRenderHook = { component ->
    component.style().font()?.value()?.let { font ->
        addStyle("font-family: $font, \"Minecraft\", monospace")
    }

    true
}

/** A render hook for text components. */
public val TEXT_RENDER_HOOK: ComponentRenderHook =
    result@{ component ->
        if (component is TextComponent) {
            text(component.content())

            // you can't manipulate the dom after setting the content of a tag, thanks kotlinx.html
            return@result false
        }

        true
    }
