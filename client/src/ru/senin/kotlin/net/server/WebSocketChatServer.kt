package ru.senin.kotlin.net.server

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.slf4j.event.Level
import ru.senin.kotlin.net.Message

class WebSocketChatServer(private val host: String, private val port: Int) : ChatServer(host, port) {
    override fun configureModule(): Application.() -> Unit = {

    }
}