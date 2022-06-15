package net.kyori.adventure.webui

import kotlinx.serialization.Serializable

@Serializable
public data class BuildInfo(
    public val startedAt: String,
    public val version: String,
    public val commit: String,
    public val bytebinInstance: String
)
