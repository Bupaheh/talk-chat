package ru.senin.kotlin.net.client

import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.senin.kotlin.net.Message
import ru.senin.kotlin.net.Protocol
import java.io.Closeable

interface ChatClient: Closeable {
    fun sendMessage(message: Message)

    override fun close() {

    }

    companion object {
        @ExperimentalCoroutinesApi
        fun create(protocol: Protocol, host: String, port: Int) = when(protocol) {
                Protocol.HTTP -> HttpChatClient(host, port)
                Protocol.WEBSOCKET -> WebSocketChatClient(host, port)
                Protocol.UDP -> UdpChatClient(host, port)
            }
    }
}