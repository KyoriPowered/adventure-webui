package net.kyori.adventure.webui.jvm

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CachingHeaders
import io.ktor.features.Compression
import io.ktor.features.deflate
import io.ktor.features.gzip
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.cio.websocket.ExperimentalWebSocketExtensionApi
import io.ktor.http.cio.websocket.WebSocketDeflateExtension
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.timeout
import io.ktor.http.content.CachingOptions
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import java.time.Duration

@OptIn(ExperimentalWebSocketExtensionApi::class)
public fun Application.main() {
    install(Compression) {
        gzip()
        deflate()
    }

    install(CachingHeaders) {
        options { outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Image.JPEG, ContentType.parse("application/x-font-woff") ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 31536000))
                ContentType.Text.CSS, ContentType.Application.JavaScript ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 86400))
                else -> null
            }
        }
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(5)

        extensions { install(WebSocketDeflateExtension) }
    }

    routing {
        // enable trace routing if in dev mode
        if (developmentMode) {
            trace { route -> log.debug(route.buildText()) }
        }
    }
}

/** Reads a string value from the `config` block in `application.conf`. */
public fun Application.getConfigString(key: String): String =
    environment.config.property("ktor.config.$key").getString()
