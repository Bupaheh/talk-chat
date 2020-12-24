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
    private val port = 8083
    val testUserName = "pupkin"
    private val testText1 = "Hi there"
    private val testText2 = ""
    private val testText3 = "Long message! Long message! Long message! Long message! Long message! " +
            "Long message! Long message! Long message! Long message! Long message! " +
            "Long message! Long message! Long message! Long message! Long message! " +
            "Long message! Long message! Long message! Long message! Long message! " +
            "Long message! Long message! Long message! Long message! Long message! " +
            "Long message! Long message! Long message! Long message! Long message! " +
            "Long message! Long message! Long message! Long message! Long message! "

    private fun test(protocol: Protocol, testText: String) {
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
    fun `HTTP sendMessage test`() = test(Protocol.HTTP, testText1)

    @Test
    fun `HTTP sendMessage blank message test`() = test(Protocol.HTTP, testText2)

    @Test
    fun `HTTP sendMessage long message test`() = test(Protocol.HTTP, testText3)

    @Test
    fun `WebSocket sendMessage test`() = test(Protocol.WEBSOCKET, testText1)

    @Test
    fun `WebSocket sendMessage blank message test`() = test(Protocol.WEBSOCKET, testText2)

    @Test
    fun `WebSocket sendMessage long message test`() = test(Protocol.WEBSOCKET, testText3)

    @Ignore
    @Test
    fun `UDP sendMessage test`() = test(Protocol.UDP, testText1)

    @Ignore
    @Test
    fun `UDP sendMessage blank message test`() = test(Protocol.UDP, testText2)

    @Ignore
    @Test
    fun `UDP sendMessage long message test`() = test(Protocol.UDP, testText3)
}
