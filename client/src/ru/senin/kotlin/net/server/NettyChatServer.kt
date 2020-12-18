package ru.senin.kotlin.net.server

import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

abstract class NettyChatServer(host: String, port: Int) : ChatServer(host, port) {
    private val engine = createEngine()

    private fun createEngine(): NettyApplicationEngine {
        val applicationEnvironment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("chat-server")
            classLoader = ApplicationEngineEnvironment::class.java.classLoader
            connector {
                this.host = this@NettyChatServer.host
                this.port = this@NettyChatServer.port
            }
            module(configureModule())
        }
        return NettyApplicationEngine(applicationEnvironment)
    }

    override fun start() {
        engine.start(true)
    }

    override fun stop() {
        engine.stop(1000, 2000)
    }

    abstract fun configureModule(): Application.() -> Unit
}

// Send test message using curl:
// curl -v -X POST http://localhost:8080/v1/message -H "Content-type: application/json" -d '{ "user":"ivanov", "text":"Hello!"}'
