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
    private val objectMapper = jacksonObjectMapper()

    override fun sendMessage(message: Message) {
        try {
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().connect(address)
            val output = socket.openWriteChannel(autoFlush = true)
            val text = objectMapper.writeValueAsString(message)
            runBlocking {
                output.writeStringUtf8(text)
            }
            socket.close()
        } catch(e: Exception) {
            println("Failed to connect to ${address.hostString}:${address.port}")
        }
    }
}