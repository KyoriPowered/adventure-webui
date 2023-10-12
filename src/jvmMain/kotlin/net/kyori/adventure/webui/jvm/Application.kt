package net.kyori.adventure.webui.jvm

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.webui.jvm.minimessage.SocketTest
import okhttp3.internal.and
import java.time.Duration

public fun Application.main() {
    install(Compression) {
        gzip()
        deflate()
    }

    install(CachingHeaders) {
        options { _, outgoingContent ->
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
            trace { route -> this@main.log.debug(route.buildText()) }
        }
    }

    SocketTest().main()
}


/** Reads a string value from the `config` block in `application.conf`. */
public fun Application.getConfigString(key: String): String =
    environment.config.property("ktor.config.$key").getString()
