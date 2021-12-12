package net.kyori.adventure.webui.jvm.minimessage

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.websocket.webSocket
import kotlinx.serialization.encodeToString
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.parser.ParsingException
import net.kyori.adventure.text.minimessage.parser.TokenParser
import net.kyori.adventure.text.minimessage.parser.node.TagNode
import net.kyori.adventure.text.minimessage.placeholder.Placeholder
import net.kyori.adventure.text.minimessage.placeholder.PlaceholderResolver
import net.kyori.adventure.text.minimessage.transformation.TransformationRegistry
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.webui.Serializers
import net.kyori.adventure.webui.URL_API
import net.kyori.adventure.webui.URL_EDITOR
import net.kyori.adventure.webui.URL_MINI_TO_HTML
import net.kyori.adventure.webui.URL_MINI_TO_JSON
import net.kyori.adventure.webui.URL_MINI_TO_TREE
import net.kyori.adventure.webui.jvm.appendComponent
import net.kyori.adventure.webui.jvm.getConfigString
import net.kyori.adventure.webui.jvm.minimessage.editor.installEditor
import net.kyori.adventure.webui.jvm.minimessage.hook.CLICK_EVENT_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.COMPONENT_CLASS_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.FONT_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.HOVER_EVENT_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.HookManager
import net.kyori.adventure.webui.jvm.minimessage.hook.INSERTION_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.TEXT_COLOR_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.TEXT_DECORATION_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.TEXT_RENDER_HOOK
import net.kyori.adventure.webui.tryDecodeFromString
import net.kyori.adventure.webui.websocket.Call
import net.kyori.adventure.webui.websocket.Combined
import net.kyori.adventure.webui.websocket.Packet
import net.kyori.adventure.webui.websocket.ParseResult
import net.kyori.adventure.webui.websocket.Placeholders
import net.kyori.adventure.webui.websocket.Response
import java.util.function.BiPredicate

public val Placeholders?.placeholderResolver: PlaceholderResolver
    get() {
        if (this == null) return PlaceholderResolver.empty()
        val stringConverted =
            this.stringPlaceholders?.map { Placeholder.miniMessage(it.key, it.value) } ?: listOf() // todo: is this correct? or do Component.text()
        val componentConverted =
            this.componentPlaceholders?.map {
                Placeholder.component(
                    it.key, GsonComponentSerializer.gson().deserialize(it.value.toString())
                )
            }
                ?: listOf()
        val miniMessageConverted =
            this.miniMessagePlaceholders?.map {
                Placeholder.component(it.key, MiniMessage.miniMessage().deserialize(it.value))
            }
                ?: listOf()
        return PlaceholderResolver.placeholders(
            stringConverted + componentConverted + miniMessageConverted
        )
    }

/** Entry-point for MiniMessage Viewer. */
public fun Application.minimessage() {
    // add standard renderers
    HookManager.apply {
        component(HOVER_EVENT_RENDER_HOOK)
        component(CLICK_EVENT_RENDER_HOOK)
        component(INSERTION_RENDER_HOOK)
        component(COMPONENT_CLASS_RENDER_HOOK)
        component(TEXT_COLOR_RENDER_HOOK)
        component(TEXT_DECORATION_RENDER_HOOK)
        component(FONT_RENDER_HOOK)
        component(TEXT_RENDER_HOOK, 500) // content needs to be set last
    }

    routing {
        // define static path to resources
        static("") {
            resources("web")
            defaultResource("web/index.html")

            val script = getConfigString("jsScriptFile")
            resource("js/main.js", script)
            resource("js/$script.map", "$script.map")
        }

        // set up other routing
        route(URL_API) {
            webSocket(URL_MINI_TO_HTML) {
                var templateResolver = PlaceholderResolver.empty()
                var miniMessage: String? = null

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        when (val packet = Serializers.json.tryDecodeFromString<Packet>(frame.readText())) {
                            is Call -> miniMessage = packet.miniMessage
                            is Placeholders -> templateResolver = packet.placeholderResolver
                            null -> continue
                        }

                        if (miniMessage == null) continue
                        val response =
                            try {
                                val result = StringBuilder()

                                miniMessage
                                    .split("\n")
                                    .map { line -> HookManager.render(line) }
                                    .map { line ->
                                        MiniMessage.miniMessage()
                                            .deserialize(line, templateResolver)
                                    }
                                    .map { component -> HookManager.render(component) }
                                    .forEach { component ->
                                        result.appendComponent(component)
                                        result.append("\n")
                                    }

                                Response(ParseResult(true, result.toString()))
                            } catch (e: Exception) {
                                Response(
                                    ParseResult(
                                        false, errorMessage = e.message ?: "Unknown error!"
                                    )
                                )
                            }

                        outgoing.send(Frame.Text(Serializers.json.encodeToString(response)))
                    }
                }
            }

            post(URL_MINI_TO_JSON) {
                val structure = Serializers.json.tryDecodeFromString<Combined>(call.receiveText())
                val input = structure?.miniMessage ?: return@post
                call.respondText(
                    GsonComponentSerializer.gson()
                        .serialize(
                            MiniMessage.miniMessage()
                                .deserialize(input, structure.placeholders.placeholderResolver)
                        )
                )
            }

            post(URL_MINI_TO_TREE) {
                val structure = Serializers.json.tryDecodeFromString<Combined>(call.receiveText())
                val input = structure?.miniMessage ?: return@post
                val resolver = structure.placeholders.placeholderResolver
                val transformationFactory = { node: TagNode ->
                    try {
                        TransformationRegistry.standard()
                            .get(
                                node.name().lowercase(),
                                node.parts(),
                                resolver,
                                Context.of(false, input, MiniMessage.miniMessage())
                            )
                    } catch (ignored: ParsingException) {
                        null
                    }
                }
                val tagNameChecker = BiPredicate { name: String?, _: Boolean ->
                    TransformationRegistry.standard().exists(name, resolver)
                }
                val root =
                    TokenParser.parse(transformationFactory, tagNameChecker, resolver, input, false)
                call.respondText(root.toString())
            }

            route(URL_EDITOR) { installEditor() }
        }
    }
}
