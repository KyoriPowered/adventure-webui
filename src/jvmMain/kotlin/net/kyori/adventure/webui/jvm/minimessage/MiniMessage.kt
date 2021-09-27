package net.kyori.adventure.webui.jvm.minimessage

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import java.util.function.BiPredicate
import kotlinx.serialization.encodeToString
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.parser.ParsingException
import net.kyori.adventure.text.minimessage.parser.TokenParser
import net.kyori.adventure.text.minimessage.parser.node.TagNode
import net.kyori.adventure.text.minimessage.transformation.TransformationRegistry
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.webui.*
import net.kyori.adventure.webui.jvm.appendComponent
import net.kyori.adventure.webui.jvm.getConfigString
import net.kyori.adventure.webui.jvm.minimessage.editor.installEditor
import net.kyori.adventure.webui.jvm.minimessage.hook.*
import net.kyori.adventure.webui.websocket.Call
import net.kyori.adventure.webui.websocket.ParseResult
import net.kyori.adventure.webui.websocket.Response

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
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val call = Serializers.json.tryDecodeFromString<Call>(frame.readText())

                        if (call?.miniMessage != null) {
                            val response =
                                try {
                                    val result = StringBuilder()

                                    call
                                        .miniMessage
                                        .split("\n")
                                        .map { line -> HookManager.render(line) }
                                        .map { line -> MiniMessage.miniMessage().deserialize(line) }
                                        .map { component -> HookManager.render(component) }
                                        .forEach { component ->
                                            result.appendComponent(component)
                                            result.append("\n")
                                        }

                                    Response(ParseResult(true, result.toString()))
                                } catch (e: Exception) {
                                    Response(
                                        ParseResult(
                                            false, errorMessage = e.message ?: "Unknown error!"))
                                }

                            outgoing.send(Frame.Text(Serializers.json.encodeToString(response)))
                        }
                    }
                }
            }

            post(URL_MINI_TO_JSON) {
                val input =
                    Serializers.json.tryDecodeFromString<Call>(call.receiveText())?.miniMessage
                        ?: return@post
                call.respondText(
                    GsonComponentSerializer.gson()
                        .serialize(MiniMessage.miniMessage().deserialize(input)))
            }

            post(URL_MINI_TO_TREE) {
                val input =
                    Serializers.json.tryDecodeFromString<Call>(call.receiveText())?.miniMessage
                        ?: return@post
                val placeholderResolver = { _: String? -> null }
                val transformationFactory = { node: TagNode ->
                    try {
                        TransformationRegistry.standard()
                            .get(
                                node.name(),
                                node.parts(),
                                mapOf(),
                                placeholderResolver,
                                Context.of(false, input, MiniMessage.miniMessage()))
                    } catch (ignored: ParsingException) {
                        null
                    }
                }
                val tagNameChecker = BiPredicate { name: String?, _: Boolean ->
                    TransformationRegistry.standard().exists(name, placeholderResolver)
                }
                val root =
                    TokenParser.parse(transformationFactory, tagNameChecker, mapOf(), input, false)
                call.respondText(root.toString())
            }

            route(URL_EDITOR) { installEditor() }
        }
    }
}
