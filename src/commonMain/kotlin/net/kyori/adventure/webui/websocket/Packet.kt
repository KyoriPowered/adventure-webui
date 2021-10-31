package net.kyori.adventure.webui.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** The client -> server call. */
public sealed interface Packet

@Serializable
@SerialName("call")
public data class Call(public val miniMessage: String? = null) : Packet

@Serializable
@SerialName("templates")
public data class Templates(
    public val stringTemplates: Map<String, String>? = null,
    public val componentTemplates: Map<String, JsonObject>? = null,
    public val miniMessageTemplates: Map<String, String>? = null
) : Packet

@Serializable
public data class Combined(
    public val miniMessage: String? = null,
    public val templates: Templates? = null
)
