package net.kyori.adventure.webui

/** A HTML data attribute. */
public class DataAttribute(
    /** The attribute in kebab case. */
    public val kebab: String
) {
    /** The attribute in camel case. */
    public val camel: String =
        kebab
            .split("-")
            .joinToString("", transform = { string -> string.replaceFirstChar(Char::uppercase) })
            .replaceFirstChar(Char::lowercase)
}
