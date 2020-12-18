package ru.senin.kotlin.net.client

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import ru.senin.kotlin.net.Message
import kotlin.concurrent.thread
import ru.senin.kotlin.net.server.ChatMessageListener
import kotlin.test.*
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.server.ChatServer
import java.lang.Thread.sleep

@ExperimentalCoroutinesApi
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
                    server.stop()
                }
            }
        })
        val serverThread = thread {
            server.start()
        }
        sleep(1000)
        ChatClient.create(protocol, host, port).use {
            it.sendMessage(Message(testUserName, testText))
            serverThread.join()
        }
        assertTrue(isReceived)
    }

    @Test
    fun `HTTP sendMessage test`() = test(Protocol.HTTP)

    @Test
    fun `WebSocket sendMessage test`() = test(Protocol.WEBSOCKET)

    @Ignore
    @Test
    fun `UDP sendMessage test`() = test(Protocol.UDP)
}
