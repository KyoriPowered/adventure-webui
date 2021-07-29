package net.kyori.adventure.webui

/** Class for DOM-rendered Adventure components. */
public const val COMPONENT_CLASS: String = "adventure-component"

/** Data name for insertion value. */
public val DATA_INSERTION: DataAttribute = DataAttribute("insertion")

/** Data name for hover event actions. */
public val DATA_HOVER_EVENT_ACTION: DataAttribute = DataAttribute("hover-event-action")

/** Data name for hover event values. */
public val DATA_HOVER_EVENT_VALUE: DataAttribute = DataAttribute("hover-event-value")

/** Data name for click event actions. */
public val DATA_CLICK_EVENT_ACTION: DataAttribute = DataAttribute("click-event-action")

/** Data name for click event values. */
public val DATA_CLICK_EVENT_VALUE: DataAttribute = DataAttribute("click-event-value")

/** Path for backend API. */
public const val URL_API: String = "/api"

/** Path for conversion between MiniMessage and HTML. */
public const val URL_MINI_TO_HTML: String = "/mini-to-html"
