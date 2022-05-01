package net.kyori.adventure.webui.jvm.minimessage.storage

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import net.kyori.adventure.webui.Serializers
import net.kyori.adventure.webui.tryDecodeFromString
import net.kyori.adventure.webui.websocket.Combined

public object BytebinStorage {
    private const val BYTEBIN_INSTANCE: String = "https://bytebin.lucko.me"

    private val client = HttpClient()

    public suspend fun bytebinStore(payload: Combined): String? {
        val response: HttpResponse = client.post("$BYTEBIN_INSTANCE/post") {
            headers {
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.UserAgent, "KyoriPowered/adventure-webui")
            }
            body = Serializers.json.encodeToString(payload)
        }
        if (response.status.isSuccess()) {
            return response.headers[HttpHeaders.Location]
        }
        return null
    }

    public suspend fun bytebinLoad(code: String): Combined? {
        val response: HttpResponse = client.get("$BYTEBIN_INSTANCE/$code") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.UserAgent, "KyoriPowered/adventure-webui")
            }
        }
        if (response.status.isSuccess()) {
            return Serializers.json.tryDecodeFromString(response.readText())
        }
        return null
    }
}
