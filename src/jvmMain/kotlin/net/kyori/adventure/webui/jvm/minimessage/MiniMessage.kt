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
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.webui.Serializers
import net.kyori.adventure.webui.URL_API
import net.kyori.adventure.webui.URL_MINI_TO_HTML
import net.kyori.adventure.webui.URL_MINI_TO_JSON
import net.kyori.adventure.webui.jvm.appendComponent
import net.kyori.adventure.webui.jvm.getConfigString
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
                                        .map { line -> MiniMessage.get().deserialize(line) }
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
                    GsonComponentSerializer.gson().serialize(MiniMessage.get().deserialize(input)))
            }
        }
    }
}
