package ru.senin.kotlin.net.client

import org.junit.jupiter.api.Test
import ru.senin.kotlin.net.Message
import kotlin.concurrent.thread
import ru.senin.kotlin.net.server.ChatMessageListener
import kotlin.test.*
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.server.ChatServer
import java.lang.Thread.sleep

class ChatTest {
    private val host = "0.0.0.0"
    private val port = 8087
    val testUserName = "pupkin"
    val testText = "Hi there"

    private fun test(protocol: Protocol) {
        val server = ChatServer.create(protocol, host, port)
        var isReceived = false
        server.setMessageListener(object: ChatMessageListener {
            override fun messageReceived(userName: String, text: String) {
                assertEquals(testUserName, userName)
                assertEquals(testText, text)
                isReceived = true
                thread {
                    sleep(1000)
                    server.stop()
                }
            }
        })
        val serverThread = thread {
            server.start()
        }
        thread {
            sleep(1000)
            ChatClient.create(protocol, host, port)
                    .sendMessage(Message(testUserName, testText))
        }
        while (serverThread.isAlive) { }
        assertTrue(isReceived)
    }

    @Test
    fun `HTTP sendMessage test`() = test(Protocol.HTTP)

    @Test
    fun `WebSocket sendMessage test`() = test(Protocol.WEBSOCKET)

    @Test
    fun `UDP sendMessage test`() = test(Protocol.UDP)
}
