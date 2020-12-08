package ru.senin.kotlin.net.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.senin.kotlin.net.Message

class WebSocketChatClient(host: String, port: Int) : ChatClient {
    private val objectMapper = jacksonObjectMapper()
    private val client = HttpClient {
        install(WebSockets)
    }
    private val channel = Channel<Message>(UNLIMITED)

    init {
        GlobalScope.launch {
            client.ws(host = host, port = port, path = "/v1/ws/message") {
                while (true) {
                    val message = channel.receive()
                    send(Frame.Text(objectMapper.writeValueAsString(message)))
                }
            }
        }
    }

    override fun sendMessage(message: Message) {
        runBlocking {
            channel.send(message)
        }
    }
}