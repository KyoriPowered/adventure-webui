package net.kyori.adventure.webui.jvm.minimessage.hook

import kotlinx.html.classes
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.webui.COMPONENT_CLASS
import net.kyori.adventure.webui.DATA_CLICK_EVENT_ACTION
import net.kyori.adventure.webui.DATA_CLICK_EVENT_VALUE
import net.kyori.adventure.webui.DATA_HOVER_EVENT_ACTION
import net.kyori.adventure.webui.DATA_HOVER_EVENT_VALUE
import net.kyori.adventure.webui.DATA_INSERTION
import net.kyori.adventure.webui.jvm.addData
import net.kyori.adventure.webui.jvm.addStyle

/** A render hook for hover events. */
public val HOVER_EVENT_RENDER_HOOK: ComponentRenderHook = { component ->
    component.hoverEvent()?.let { hoverEvent ->
        addData(DATA_HOVER_EVENT_ACTION, hoverEvent.action().toString())
        addData(DATA_HOVER_EVENT_VALUE, hoverEvent.value().toString())
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

/** A render hook for text coloring. */
public val TEXT_COLOR_RENDER_HOOK: ComponentRenderHook = { component ->
    component.color()?.let { color -> addStyle("color: ${color.asHexString()}") }

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

/** A render hook for text components. */
public val TEXT_RENDER_HOOK: ComponentRenderHook = { component ->
    if (component is TextComponent) {
        text(component.content())
    }

    true
}
