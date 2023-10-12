package net.kyori.adventure.webui.jvm.minimessage

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import okhttp3.internal.and
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.text.Charsets.UTF_8

public class SocketTest {

    public fun main() {
        // TODO
        // 1. make this non blocking somehow, idk how kotlin works
        // 2. add api/ui to store into some cache
        // 3. parse server address to get stuff from the cache and return that in the status response
        runBlocking {
            val serverSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().bind("127.0.0.1", 9002)
            println("Server is listening at ${serverSocket.localAddress}")
            while (true) {
                val socket = serverSocket.accept()
                println("Accepted ${socket.remoteAddress}")
                launch {
                    try {
                        val receiveChannel = socket.openReadChannel()
                        val sendChannel = socket.openWriteChannel(autoFlush = true)

                        // handshake
                        val handshakePacket = receiveChannel.readMcPacket()
                        val protocolVersion = handshakePacket.readVarInt()
                        val serverAddress = handshakePacket.readUtf8String()
                        val serverPort = handshakePacket.readShort()
                        val nextState = handshakePacket.readVarInt()

                        if (nextState != 1) {
                            // send kick
                            sendChannel.writeMcPacket(0) {
                                it.writeString(
                                    GsonComponentSerializer.gson()
                                        .serialize(MiniMessage.miniMessage().deserialize("<red>You cant join here!"))
                                )
                            }
                        } else {
                            // send status response
                            sendChannel.writeMcPacket(0) {
                                it.writeString(
                                    """{
                                  "version": {
                                    "name": "${
                                        LegacyComponentSerializer.legacySection()
                                            .serialize(MiniMessage.miniMessage().deserialize("<rainbow>MiniMessage"))
                                    }",
                                    "protocol": 762
                                  },
                                  "description": ${
                                        GsonComponentSerializer.gson().serialize(
                                            MiniMessage.miniMessage().deserialize("<rainbow>MiniMessage is cool!")
                                        )
                                    }
                                }""".trimIndent()
                                )
                            }
                        }

                        sendChannel.close()
                    } catch (e: Exception) {
                        println(e)
                    }

                    socket.close()
                    return@launch
                }
            }
        }
    }
}

public suspend fun ByteWriteChannel.writeMcPacket(packetId: Int, consumer: (packet: DataOutputStream) -> Unit) {
    val stream = ByteArrayOutputStream()
    val packet = DataOutputStream(stream)

    consumer.invoke(packet)

    val data = stream.toByteArray()
    writeVarInt(data.size + 1)
    writeVarInt(packetId)
    writeFully(data)
}

public fun DataOutputStream.writeString(string: String) {
    val bytes = string.toByteArray(UTF_8)
    writeVarInt(bytes.size)
    write(bytes)
}

public fun DataOutputStream.writeVarInt(int: Int) {
    var value = int
    while (true) {
        if ((value and 0x7F.inv()) == 0) {
            writeByte(value)
            return
        }

        writeByte((value and 0x7F) or 0x80)

        value = value ushr 7
    }
}

public suspend fun ByteWriteChannel.writeVarInt(int: Int) {
    var value = int
    while (true) {
        if ((value and 0x7F.inv()) == 0) {
            writeByte(value)
            return
        }

        writeByte((value and 0x7F) or 0x80)

        value = value ushr 7
    }
}

public suspend fun ByteReadChannel.readMcPacket(): ByteReadChannel {
    val length = readVarInt()
    val packetId = readVarInt()
    val data = ByteArray(length)
    readFully(data, 0, length)
    return ByteReadChannel(data)
}

public suspend fun ByteReadChannel.readVarInt(): Int {
    var value = 0
    var position = 0
    var currentByte: Byte

    while (true) {
        currentByte = readByte()
        value = value or ((currentByte and 0x7F) shl position)

        if ((currentByte and 0x80) == 0) break

        position += 7

        if (position >= 32) throw RuntimeException("VarInt is too big")
    }

    return value
}

public suspend fun ByteReadChannel.readUtf8String(): String {
    val length = readVarInt()
    val data = ByteArray(length)
    readFully(data, 0, length)
    return String(data)
}
