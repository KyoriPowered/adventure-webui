@file:OptIn(kotlin.time.ExperimentalTime::class)

package net.kyori.adventure.webui.jvm.minimessage.editor

import io.github.reactivecircus.cache4k.Cache
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.encodeToString
import net.kyori.adventure.webui.Serializers
import net.kyori.adventure.webui.URL_EDITOR_INPUT
import net.kyori.adventure.webui.URL_EDITOR_OUTPUT
import net.kyori.adventure.webui.editor.EditorInput
import net.kyori.adventure.webui.tryDecodeFromString
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

private val inputSessions: Cache<String, EditorInput> =
    Cache.Builder<String, EditorInput>().expireAfterWrite(5.minutes).build()
private val outputSessions: Cache<String, String> =
    Cache.Builder<String, String>().expireAfterWrite(5.minutes).build()

/** Installs routes for the editor system */
public fun Route.installEditor() {
    post(URL_EDITOR_INPUT) {
        val input = Serializers.json.tryDecodeFromString<EditorInput>(call.receiveText())
        if (input == null) {
            call.response.status(HttpStatusCode.BadRequest)
        } else {
            generateToken().let { token ->
                inputSessions.put(token, input)
                call.respondText(
                    Serializers.json.encodeToString(EditorInputResponse(token)),
                    ContentType.Application.Json
                )
            }
        }
    }

    get(URL_EDITOR_INPUT) {
        val token = call.parameters["token"]
        val input = token?.let(inputSessions::get)
        if (input == null) {
            call.response.status(HttpStatusCode.NotFound)
        } else {
            inputSessions.invalidate(token)
            call.respondText(Serializers.json.encodeToString(input))
        }
    }

    post(URL_EDITOR_OUTPUT) {
        generateToken().let { token ->
            outputSessions.put(token, call.receiveText())
            call.respondText(token)
        }
    }

    get(URL_EDITOR_OUTPUT) {
        val token = call.parameters["token"]
        val output = token?.let(outputSessions::get)
        if (output == null) {
            call.response.status(HttpStatusCode.NotFound)
        } else {
            outputSessions.invalidate(output)
            call.respondText(output)
        }
    }
}

private fun generateToken(): String = UUID.randomUUID().toString().replace("-", "")
