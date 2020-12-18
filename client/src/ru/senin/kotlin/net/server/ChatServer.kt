package ru.senin.kotlin.net.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ru.senin.kotlin.net.Protocol

interface ChatMessageListener {
    fun messageReceived(userName: String, text: String)
}

abstract class ChatServer(protected val host: String, protected val port: Int) {
    protected val objectMapper = jacksonObjectMapper()
    protected var listener: ChatMessageListener? = null

    abstract fun start()

    abstract fun stop()

    fun setMessageListener(listener: ChatMessageListener) {
        this.listener = listener
    }

    companion object {
        fun create(protocol: Protocol, host: String, port: Int) = when (protocol) {
            Protocol.HTTP -> HttpChatServer(host, port)
            Protocol.WEBSOCKET -> WebSocketChatServer(host, port)
            Protocol.UDP -> UdpChatServer(host, port)
        }
    }
}

// Send test message using curl:
// curl -v -X POST http://localhost:8080/v1/message -H "Content-type: application/json" -d '{ "user":"ivanov", "text":"Hello!"}'
