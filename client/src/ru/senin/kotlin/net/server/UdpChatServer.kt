package ru.senin.kotlin.net.server

import io.ktor.application.*

class UdpChatServer(private val host: String, private val port: Int) : ChatServer(host, port) {
    override fun configureModule(): Application.() -> Unit = {

    }
}