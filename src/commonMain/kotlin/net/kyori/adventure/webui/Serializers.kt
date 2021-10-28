package net.kyori.adventure.webui

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** The serializers. */
public object Serializers {
    /** The Json serializer. */
    public var json: Json = Json
}

/** Attempts to decode a value from a string, returning `null` if the decoding failed. */
public inline fun <reified T> Json.tryDecodeFromString(string: String): T? =
    try {
        decodeFromString<T>(string)
    } catch (_: SerializationException) {
        null
    }
