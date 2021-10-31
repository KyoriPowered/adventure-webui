package net.kyori.adventure.webui

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.kyori.adventure.webui.websocket.Call
import net.kyori.adventure.webui.websocket.Packet
import net.kyori.adventure.webui.websocket.Templates

/** The serializers. */
public object Serializers {
    private val module = SerializersModule {
        polymorphic(Packet::class) {
            subclass(Call::class)
            subclass(Templates::class)
        }
    }

    /** The Json serializer. */
    public val json: Json = Json { serializersModule = module }
}

/** Attempts to decode a value from a string, returning `null` if the decoding failed. */
public inline fun <reified T> Json.tryDecodeFromString(string: String): T? =
    try {
        decodeFromString<T>(string)
    } catch (_: SerializationException) {
        null
    }
