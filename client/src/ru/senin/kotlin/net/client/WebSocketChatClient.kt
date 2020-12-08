package ru.senin.kotlin.net.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.receiveOrNull
import ru.senin.kotlin.net.Message

@ExperimentalCoroutinesApi
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
                    if (outgoing.isClosedForSend)
                        println("Failed to connect to $host:$port")
                    else {
                        send(Frame.Text(objectMapper.writeValueAsString(message)))
                        val response = incoming.receiveOrNull() ?: Frame.Text("!OK")
                        if (response !is Frame.Text || response.readText() != "OK")
                            println("Message wasn't received")
                    }
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