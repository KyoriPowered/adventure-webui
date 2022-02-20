package net.kyori.adventure.webui.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** The client -> server call. */
public sealed interface Packet

@Serializable
@SerialName("call")
public data class Call(
    public val miniMessage: String? = null,
    public val isolateNewlines: Boolean = false
) : Packet

@Serializable
@SerialName("placeholders")
public data class Placeholders(
    public val stringPlaceholders: Map<String, String>? = null,
    public val componentPlaceholders: Map<String, JsonObject>? = null
) : Packet

@Serializable
public data class Combined(
    public val miniMessage: String? = null,
    public val placeholders: Placeholders? = null
)
