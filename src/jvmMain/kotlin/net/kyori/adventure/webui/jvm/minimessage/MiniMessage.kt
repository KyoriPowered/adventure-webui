package net.kyori.adventure.webui.jvm.minimessage

import io.ktor.application.Application
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.websocket.webSocket
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.webui.URL_API
import net.kyori.adventure.webui.URL_MINI_TO_HTML
import net.kyori.adventure.webui.jvm.appendComponent
import net.kyori.adventure.webui.jvm.getConfigString
import net.kyori.adventure.webui.jvm.minimessage.hook.CLICK_EVENT_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.COMPONENT_CLASS_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.HOVER_EVENT_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.HookManager
import net.kyori.adventure.webui.jvm.minimessage.hook.INSERTION_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.TEXT_COLOR_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.TEXT_DECORATION_RENDER_HOOK
import net.kyori.adventure.webui.jvm.minimessage.hook.TEXT_RENDER_HOOK

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
        component(TEXT_RENDER_HOOK, 500) // content needs to be set last
    }

    routing {
        // define static path to resources
        static("") {
            resources("web")
            resource("js/main.js", getConfigString("jsScriptFile"))
            defaultResource("web/index.html")
        }

        // set up other routing
        route(URL_API) {
            webSocket(URL_MINI_TO_HTML) {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val result = StringBuilder()

                        frame
                            .readText()
                            .split("\n")
                            .map { line -> HookManager.render(line) }
                            .map { line -> MiniMessage.get().deserialize(line) }
                            .map { component -> HookManager.render(component) }
                            .forEach { component ->
                                result.appendComponent(component)
                                result.append("\n")
                            }

                        outgoing.send(Frame.Text(result.toString()))
                    }
                }
            }
        }
    }
}
