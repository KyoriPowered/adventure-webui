package net.kyori.adventure.webui.jvm.minimessage

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receiveText
import io.ktor.response.respondTextWriter
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.webui.URL_API
import net.kyori.adventure.webui.URL_MINI_TO_HTML
import net.kyori.adventure.webui.jvm.appendComponent
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
        component(TEXT_RENDER_HOOK)
    }

    routing {
        // define static path to resources
        static("") {
            resources("web")
            resource(
                "js/main.js", environment.config.property("ktor.config.jsScriptFile").getString())
            defaultResource("web/index.html")
        }

        // set up other routing
        route(URL_API) {
            post(URL_MINI_TO_HTML) {
                call.respondTextWriter {
                    call
                        .receiveText()
                        .split("\n")
                        .map { line -> HookManager.render(line) }
                        .map { line -> MiniMessage.get().deserialize(line) }
                        .map { component -> HookManager.render(component) }
                        .forEach { component ->
                            appendComponent(component)
                            append("\n")
                        }
                }
            }
        }
    }
}
