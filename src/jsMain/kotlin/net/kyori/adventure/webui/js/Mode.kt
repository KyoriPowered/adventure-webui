package net.kyori.adventure.webui.js

/** Display modes in which text can be rendered. */
public enum class Mode {
    CHAT_OPEN,
    CHAT_CLOSED,
    LORE,
    HOLOGRAM,
    SERVER_LIST,
    ;

    /** The mode as a HTML class. */
    public val className: String = "mode-${name.lowercase().replace('_', '-')}"

    /** The mode as a URL parameter. */
    public val paramName: String = name.lowercase()

    public companion object {
        /** A collection of all modes. */
        public val MODES: Collection<Mode> = values().asList()

        private val DEFAULT: Mode = CHAT_CLOSED
        private val INDEX: Map<String, Mode> = MODES.associateBy { mode -> mode.name }

        /** Gets a mode from [string], returning [CHAT_CLOSED] as a default. */
        public fun fromString(string: String?): Mode = INDEX[string?.uppercase()] ?: DEFAULT
    }
}
