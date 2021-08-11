package net.kyori.adventure.webui.websocket

import kotlinx.serialization.Serializable

/** The server -> client response. */
@Serializable public data class Response(public val parseResult: ParseResult? = null)

/** The result of a parse. */
@Serializable
public data class ParseResult(
    /** If the parse was a success. */
    public val success: Boolean,
    /** The result of the conversion, only if it was a [success]. */
    public val dom: String? = null,
    /** The error message, if it wasn't a [success]. */
    public val errorMessage: String? = null
)
