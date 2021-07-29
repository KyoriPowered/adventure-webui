package net.kyori.adventure.webui.jvm.minimessage.hook

import kotlinx.html.SPAN
import net.kyori.adventure.text.Component

/** A map of render hooks. */
public class RenderHooks<T, U : RenderHook<T>>(list: MutableList<U> = mutableListOf()) :
    MutableList<U> by list {

    /** Renders some input using all stored render hooks. */
    public fun render(input: T): T {
        var result = input

        forEach { hook -> result = hook(result) }

        return result
    }
}

/** A hook that mutates a value of a type. */
public typealias RenderHook<T> = (T) -> T

/** A hook that is executed before rendering. */
public typealias PreRenderHook = RenderHook<String>

/** A hook that is executed after rendering. */
public typealias PostRenderHook = RenderHook<Component>

/**
 * A hook that renders a component to the DOM, returning if rendering should continue down the
 * queue.
 */
public typealias ComponentRenderHook = SPAN.(Component) -> Boolean
