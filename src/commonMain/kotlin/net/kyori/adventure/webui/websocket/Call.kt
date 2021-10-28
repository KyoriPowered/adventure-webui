package net.kyori.adventure.webui.websocket

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** The client -> server call. */
@Serializable
public data class Call(
    /** A MiniMessage string to parse. */
    public val miniMessage: String? = null,
    public val stringTemplates: Map<String, String>? = null,
    public val componentTemplates: Map<String, JsonObject>? = null
)
