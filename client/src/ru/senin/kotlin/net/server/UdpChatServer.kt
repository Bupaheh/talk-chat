package ru.senin.kotlin.net.server

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.senin.kotlin.net.Message
import java.net.InetSocketAddress

class UdpChatServer(host: String, port: Int) : ChatServer(host, port) {
    private val server = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress("127.0.0.1", 2323))

    override fun start() {
        runBlocking {
            for (datagram in server.incoming) {
                val text = datagram.packet.readText()
                val message = objectMapper.readValue<Message>(text)
                listener?.messageReceived(message.user, message.text)
            }
        }
    }

    override fun stop() {
    }
}