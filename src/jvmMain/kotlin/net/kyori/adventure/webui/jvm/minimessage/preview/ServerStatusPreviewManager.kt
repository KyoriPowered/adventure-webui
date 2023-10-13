package net.kyori.adventure.webui.jvm.minimessage.preview

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.server.application.Application
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeByte
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import okhttp3.internal.and
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.coroutines.CoroutineContext

/** Manager class for previewing server status. */
public class ServerStatusPreviewManager(
    application: Application,
) : CoroutineScope {

    private val logger = LoggerFactory.getLogger(ServerStatusPreviewManager::class.java)
    private val managerJob = SupervisorJob(application.coroutineContext.job)
    override val coroutineContext: CoroutineContext = application.coroutineContext + managerJob

    init {
        launch {
            // Initialise the socket.
            val serverSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().bind("127.0.0.1", 9002)
            logger.info("Listening for pings at ${serverSocket.localAddress}")

            while (true) {
                // Ensure we are active so that the socket is properly closed when the application ends.
                ensureActive()

                val socket = serverSocket.accept()
                logger.debug("Accepted socket connection from {}", socket.remoteAddress)

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
                                    "protocol": $protocolVersion
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
                        logger.error("An unknown error occurred whilst responding to a ping from ${socket.remoteAddress}", e)
                    }

                    socket.close()
                }
            }
        }
    }

    private suspend fun ByteWriteChannel.writeMcPacket(packetId: Int, consumer: (packet: DataOutputStream) -> Unit) {
        val stream = ByteArrayOutputStream()
        val packet = DataOutputStream(stream)

        consumer.invoke(packet)

        val data = stream.toByteArray()
        writeVarInt(data.size + 1)
        writeVarInt(packetId)
        writeFully(data)
    }

    private fun DataOutputStream.writeString(string: String) {
        val bytes = string.toByteArray(Charsets.UTF_8)
        writeVarInt(bytes.size)
        write(bytes)
    }

    private fun DataOutputStream.writeVarInt(int: Int) {
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

    private suspend fun ByteWriteChannel.writeVarInt(int: Int) {
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

    private suspend fun ByteReadChannel.readMcPacket(): ByteReadChannel {
        val length = readVarInt()
        val packetId = readVarInt()
        val data = ByteArray(length)
        readFully(data, 0, length)
        return ByteReadChannel(data)
    }

    private suspend fun ByteReadChannel.readVarInt(): Int {
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

    private suspend fun ByteReadChannel.readUtf8String(): String {
        val length = readVarInt()
        val data = ByteArray(length)
        readFully(data, 0, length)
        return String(data)
    }
}
