package net.kyori.adventure.webui.jvm

import io.ktor.application.Application
import io.ktor.application.log
import io.ktor.routing.routing

public fun Application.main() {
    // routing
    routing {
        // enable trace routing if in dev mode
        if (developmentMode) {
            trace { route -> log.debug(route.buildText()) }
        }
    }
}
