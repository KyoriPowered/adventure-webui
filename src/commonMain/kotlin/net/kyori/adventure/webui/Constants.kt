package net.kyori.adventure.webui

/** Class for DOM-rendered Adventure components. */
public const val COMPONENT_CLASS: String = "adventure-component"

/** Data name for insertion value. */
public val DATA_INSERTION: DataAttribute = DataAttribute("insertion")

/** Data name for "show text" hover event values. */
public val DATA_HOVER_EVENT_SHOW_TEXT: DataAttribute = DataAttribute("hover-event-show-text")

/** Data name for click event actions. */
public val DATA_CLICK_EVENT_ACTION: DataAttribute = DataAttribute("click-event-action")

/** Data name for click event values. */
public val DATA_CLICK_EVENT_VALUE: DataAttribute = DataAttribute("click-event-value")

/** Path for backend API. */
public const val URL_API: String = "/api"

/** Path for conversion between MiniMessage and HTML. */
public const val URL_MINI_TO_HTML: String = "/mini-to-html"

/** Path for conversion between MiniMessage and JSON. */
public const val URL_MINI_TO_JSON: String = "/mini-to-json"

/** Path for conversion between MiniMessage and a tree view. */
public const val URL_MINI_TO_TREE: String = "/mini-to-tree"

/** Path for editor-related API routes. */
public const val URL_EDITOR: String = "/editor"

/** Path for editor-related input routes. */
public const val URL_EDITOR_INPUT: String = "/input"

/** Path for editor-related output routes. */
public const val URL_EDITOR_OUTPUT: String = "/output"

/** Parameter for obtaining editor data. */
public const val PARAM_EDITOR_TOKEN: String = "token"

/** Path for getting a short link for a MiniMessage input. */
public const val URL_MINI_SHORTEN: String = "/mini-shorten"

/** Path for getting a hostname for an in-game MiniMessage motd preview. */
public const val URL_SETUP_MOTD_PREVIEW: String = "/setup-motd-preview"
/** Path for getting a hostname for an in-game MiniMessage kick preview. */
public const val URL_SETUP_KICK_PREVIEW: String = "/setup-kick-preview"

/** Path for getting the configuration of this WebUI instance */
public const val URL_BUILD_INFO: String = "/build"
