package net.kyori.adventure.webui.editor

import kotlinx.serialization.Serializable

/** The input to initiate an editor session. */
@Serializable
public data class EditorInput(
    /** The MiniMessage input string. */
    public val input: String,
    /**
     * The command to use to "save" this editor. The string "{token}" will be replaced with the
     * token that you can use to request the data back.
     */
    public val command: String,
    /** The name of your application to display to the user. */
    public val application: String
)
