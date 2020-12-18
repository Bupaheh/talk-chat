package ru.senin.kotlin.net.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ru.senin.kotlin.net.Message
import java.net.InetSocketAddress

class UdpChatClient(host: String, port: Int) : ChatClient {
    private val address = InetSocketAddress(host, port)
    private val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().connect(address)
    private val output = socket.openWriteChannel(autoFlush = true)
    private val objectMapper = jacksonObjectMapper()

    override fun sendMessage(message: Message) {
        if (socket.isClosed)
            println("Failed to connect to ${address.hostString}:${address.port}")
        else {
            val text = objectMapper.writeValueAsString(message)
            runBlocking {
                output.writeStringUtf8(text)
            }
        }
    }

    override fun close() {
        socket.close()
    }
}