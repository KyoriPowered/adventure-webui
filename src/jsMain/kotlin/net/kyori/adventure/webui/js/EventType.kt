package net.kyori.adventure.webui.js

/** Types of Adventure events. */
public enum class EventType {
    CLICK,
    HOVER,
    INSERTION,
    ;

    /** Checks if this event type is usable in a given mode. */
    public fun isUsable(mode: Mode): Boolean = mode == Mode.CHAT_OPEN || mode == Mode.CHAT_CLOSED

    public companion object {
        /** A collection of all event types. */
        public fun all(): Collection<EventType> = setOf(CLICK, INSERTION, HOVER)
    }
}
