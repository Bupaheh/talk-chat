package ru.senin.kotlin.net.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import ru.senin.kotlin.net.Message

class WebSocketChatServer(private val host: String, private val port: Int) : ChatServer(host, port) {
    override fun configureModule(): Application.() -> Unit = {
        install(WebSockets)

        routing {
            webSocket("/v1/ws/message") {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val message = objectMapper.readValue<Message>(text)
                        listener?.messageReceived(message.user, message.text)
                        outgoing.send(Frame.Text("OK"))
                    }
                }
            }
            webSocket("/") {

            }
        }
    }
}