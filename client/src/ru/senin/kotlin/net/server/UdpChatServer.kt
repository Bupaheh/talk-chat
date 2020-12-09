package ru.senin.kotlin.net.server

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ru.senin.kotlin.net.Message
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.UdpHealthCheckData
import ru.senin.kotlin.net.UserAddress
import java.net.InetSocketAddress

open class UdpChatServer(host: String, port: Int) : ChatServer(host, port) {
    private val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress(host, port))

    protected open suspend fun healthCheck(text: String) {
        try {
            val data = objectMapper.readValue<UdpHealthCheckData>(text)
            val socket = aSocket(ActorSelectorManager(Dispatchers.IO))
                .udp().connect(InetSocketAddress(data.host, data.port))
            val output = socket.openWriteChannel(autoFlush = true)
            val response = objectMapper.writeValueAsString(UdpHealthCheckData(host, port, data.id))
            output.writeStringUtf8(objectMapper.writeValueAsString(Message("healthCheck", response)))
        } catch(e: Exception) { }
    }

    override fun start() {
        runBlocking {
            for (datagram in socket.incoming) {
                val text = datagram.packet.readText()
                val message = objectMapper.readValue<Message>(text)
                if (message.user == "healthCheck")
                    healthCheck(message.text)
                else
                    listener?.messageReceived(message.user, message.text)
            }
        }
    }

    override fun stop() {
        socket.close()
    }
}