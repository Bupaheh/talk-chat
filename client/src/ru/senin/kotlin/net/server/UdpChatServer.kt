package ru.senin.kotlin.net.server

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ru.senin.kotlin.net.Message
import java.net.InetSocketAddress

class UdpChatServer(host: String, port: Int) : ChatServer(host, port) {
    private val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress(host, port))

    override fun start() {
        runBlocking {
            for (datagram in socket.incoming) {
                val text = datagram.packet.readText()
                val message = objectMapper.readValue<Message>(text)
                listener?.messageReceived(message.user, message.text)
            }
        }
    }

    override fun stop() {
    }
}