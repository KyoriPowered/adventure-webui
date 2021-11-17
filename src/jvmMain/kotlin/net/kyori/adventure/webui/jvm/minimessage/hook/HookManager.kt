package net.kyori.adventure.webui.jvm.minimessage.hook

import kotlinx.html.SPAN
import net.kyori.adventure.text.Component
import java.util.PriorityQueue

/** Manager for hooks. */
public object HookManager {
    private val preHooks: RenderHooks<String, PreRenderHook> = RenderHooks()
    private val postHooks: RenderHooks<Component, PostRenderHook> = RenderHooks()
    private val typeHooks: PriorityQueue<PrioritisedComponentRenderHook> = PriorityQueue()

    /** Adds a pre-render hook. */
    public fun pre(hook: PreRenderHook) {
        preHooks.add(hook)
    }

    /** Adds a post-render hook. */
    public fun post(hook: PostRenderHook) {
        postHooks.add(hook)
    }

    /** Adds a component render hook. */
    public fun component(hook: ComponentRenderHook, priority: Int = 0) {
        typeHooks.add(PrioritisedComponentRenderHook(hook, priority))
    }

    /** Renders [input] using the pre-render hooks. */
    public fun render(input: String): String = preHooks.render(input)

    /** Renders [input] using the post-render hooks. */
    public fun render(input: Component): Component = postHooks.render(input)

    /** Renders [input] to [span] using the component render hooks. */
    public fun render(span: SPAN, input: Component) {
        typeHooks.forEach { hook -> hook.render(span, input) }
    }
}

/** A component render hook with an arbitrary priority. */
private class PrioritisedComponentRenderHook(val render: ComponentRenderHook, val priority: Int) :
    Comparable<PrioritisedComponentRenderHook> {

    override fun compareTo(other: PrioritisedComponentRenderHook): Int =
        priority.compareTo(other.priority)
}
