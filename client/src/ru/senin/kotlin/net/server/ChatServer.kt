package ru.senin.kotlin.net.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

interface ChatMessageListener {
    fun messageReceived(userName: String, text: String)
}

abstract class ChatServer(private val host: String, private val port: Int) {
    private val objectMapper = jacksonObjectMapper()
    protected var listener: ChatMessageListener? = null

    private val engine = createEngine()

    private fun createEngine(): NettyApplicationEngine {
        val applicationEnvironment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("chat-server")
            classLoader = ApplicationEngineEnvironment::class.java.classLoader
            connector {
                this.host = this@ChatServer.host
                this.port = this@ChatServer.port
            }
            module(configureModule())
        }
        return NettyApplicationEngine(applicationEnvironment)
    }

    fun start() {
        engine.start(true)
    }

    fun stop() {
        engine.stop(1000, 2000)
    }

    fun setMessageListener(listener: ChatMessageListener) {
        this.listener = listener
    }

    abstract fun configureModule(): Application.() -> Unit
}

// Send test message using curl:
// curl -v -X POST http://localhost:8080/v1/message -H "Content-type: application/json" -d '{ "user":"ivanov", "text":"Hello!"}'
