@file:OptIn(kotlin.time.ExperimentalTime::class)

package net.kyori.adventure.webui.jvm.minimessage.editor

import io.github.reactivecircus.cache4k.Cache
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.serialization.encodeToString
import net.kyori.adventure.webui.Serializers
import net.kyori.adventure.webui.URL_EDITOR_INPUT
import net.kyori.adventure.webui.URL_EDITOR_OUTPUT
import net.kyori.adventure.webui.editor.EditorInput
import net.kyori.adventure.webui.tryDecodeFromString
import java.util.UUID
import kotlin.time.Duration

private val inputSessions: Cache<String, EditorInput> =
    Cache.Builder().expireAfterWrite(Duration.minutes(5)).build()
private val outputSessions: Cache<String, String> =
    Cache.Builder().expireAfterWrite(Duration.minutes(5)).build()

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
