package net.kyori.adventure.webui.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** The client -> server call. */
public sealed interface Packet

public sealed interface Templates : Packet {
    public val stringTemplates: Map<String, String>?
    public val componentTemplates: Map<String, JsonObject>?
}

@Serializable
@SerialName("call")
public data class Call(public val miniMessage: String? = null) : Packet

@Serializable
@SerialName("templates")
public data class TemplatesImpl(
    public override val stringTemplates: Map<String, String>? = null,
    public override val componentTemplates: Map<String, JsonObject>? = null
) : Templates

@Serializable
public data class Combined(
    public val miniMessage: String? = null,
    public override val stringTemplates: Map<String, String>? = null,
    public override val componentTemplates: Map<String, JsonObject>? = null
) : Templates
