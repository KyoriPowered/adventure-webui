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

/** Reads a string value from the `config` block in `application.conf`. */
public fun Application.getConfigString(key: String): String = environment.config.property("ktor.config.$key").getString()
