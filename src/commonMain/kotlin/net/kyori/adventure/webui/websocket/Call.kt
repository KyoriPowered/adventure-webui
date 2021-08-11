package net.kyori.adventure.webui.websocket

import kotlinx.serialization.Serializable

/** The client -> server call. */
@Serializable
public data class Call(
    /** A MiniMessage string to parse. */
    public val miniMessage: String? = null
)
