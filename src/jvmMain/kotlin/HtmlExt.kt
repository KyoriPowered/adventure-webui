package net.kyori.adventure.webui.jvm

import kotlinx.html.HtmlBlockTag
import kotlinx.html.TagConsumer
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import net.kyori.adventure.text.Component
import net.kyori.adventure.webui.DataAttribute
import net.kyori.adventure.webui.jvm.minimessage.hook.HookManager

/** Appends a component in DOM form to [O]. */
public fun <O : Appendable> O.appendComponent(
    component: Component,
    prettyPrint: Boolean = false,
    xhtmlCompatible: Boolean = false
) {
    appendHTML(prettyPrint, xhtmlCompatible).appendComponent(component)
}

/** Adds a data attribute with a given value. */
public fun HtmlBlockTag.addData(attribute: DataAttribute, value: String) {
    attributes["data-${attribute.kebab}"] = value
}

/** Adds a CSS style. */
public fun HtmlBlockTag.addStyle(style: String) {
    if (attributes["style"].isNullOrBlank()) {
        attributes["style"] = style
    } else {
        attributes["style"] = attributes["style"] + ";$style"
    }
}

private fun <T, C : TagConsumer<T>> C.appendComponent(component: Component) {
    span {
        // delegate rendering to the hook manager
        HookManager.render(this, component)

        // recurse to render children
        component.children().forEach { child -> appendComponent(child) }
    }
}
