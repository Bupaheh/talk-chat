package ru.senin.kotlin.net.server

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import ru.senin.kotlin.net.Message
import ru.senin.kotlin.net.UdpHealthCheckData
import java.net.InetSocketAddress

open class UdpChatServer(host: String, port: Int) : ChatServer(host, port) {
    private val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).udp().bind(InetSocketAddress(host, port))
    lateinit var serverJob: Job

    protected open suspend fun healthCheck(text: String) {
        val data = objectMapper.readValue<UdpHealthCheckData>(text)
        val healthCheckSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
            .udp().connect(InetSocketAddress(data.host, data.port))
        healthCheckSocket.use {
            val output = it.openWriteChannel(autoFlush = true)
            val response = objectMapper.writeValueAsString(UdpHealthCheckData(host, port, data.id))
            output.writeStringUtf8(objectMapper.writeValueAsString(Message("healthCheck", response)))
        }
    }

    override fun start() {
        runBlocking {
            serverJob = launch {
                socket.use {
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
        }
    }

    override fun stop() {
        runBlocking {
            serverJob.cancelAndJoin()
        }
    }
}