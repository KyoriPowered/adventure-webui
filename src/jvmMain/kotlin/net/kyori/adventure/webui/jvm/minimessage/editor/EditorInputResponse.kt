package net.kyori.adventure.webui.jvm.minimessage.editor

import kotlinx.serialization.Serializable

/** The response to an editor input request. */
@Serializable
public data class EditorInputResponse(
    /** The token used to access the editor session. */
    public val token: String
)
