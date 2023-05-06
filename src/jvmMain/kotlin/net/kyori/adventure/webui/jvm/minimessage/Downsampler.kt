package net.kyori.adventure.webui.jvm.minimessage

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import java.util.function.UnaryOperator

// This could just be thing which serializes to legacy, and then back. But this thing is way more fun
private class LimitedDownsampler(val colors: List<TextColor>) {
    fun render(component: Component): Component {
        return component
            .children(component.children().map(::render))
            .color(component.color()?.let { color -> TextColor.nearestColorTo(colors, color) })
            .hoverEvent(null)
            .clickEvent(null)
    }
}

// Won't be necessary after https://github.com/KyoriPowered/adventure/issues/910 is closed
private val bedrockColors = listOf(
    NamedTextColor.DARK_BLUE,
    NamedTextColor.DARK_GREEN,
    NamedTextColor.DARK_AQUA,
    NamedTextColor.DARK_RED,
    NamedTextColor.DARK_PURPLE,
    NamedTextColor.GOLD,
    NamedTextColor.GRAY,
    NamedTextColor.DARK_GRAY,
    NamedTextColor.BLUE,
    NamedTextColor.GREEN,
    NamedTextColor.AQUA,
    NamedTextColor.RED,
    NamedTextColor.LIGHT_PURPLE,
    NamedTextColor.YELLOW,
    NamedTextColor.WHITE,
    TextColor.color(0xddd605), // minecoin_gold
    TextColor.color(0xe3d4d1), // material_quartz
    TextColor.color(0xcecaca), // material_iron
    TextColor.color(0x443a3b), // material_netherite
    TextColor.color(0x971607), // material_redstone
    TextColor.color(0xb4684d), // material_copper
    TextColor.color(0xdEB12d), // material_gold
    TextColor.color(0x47a036), // material_emerald
    TextColor.color(0x2cbaa8), // material_diamond
    TextColor.color(0x21497b), // material_lapis
    TextColor.color(0x9a5cc6), // material_amethyst
)
private val legacyDownsampler = LimitedDownsampler(NamedTextColor.NAMES.values().toList())
private val bedrockDownsampler = LimitedDownsampler(bedrockColors)

public enum class Downsampler(private val function: UnaryOperator<Component>) {
    NONE(UnaryOperator.identity()),
    LEGACY(legacyDownsampler::render),
    BEDROCK(bedrockDownsampler::render),
    ;

    public fun apply(component: Component): Component {
        return function.apply(component).compact()
    }

    public companion object {
        private val values = Downsampler.values()
        public fun of(name: String?): Downsampler {
            return values.firstOrNull { i -> i.name.equals(name, ignoreCase = true) } ?: NONE
        }
    }
}
