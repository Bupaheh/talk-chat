package ru.senin.kotlin.net.client

import org.junit.jupiter.api.Test
import ru.senin.kotlin.net.Message
import ru.senin.kotlin.net.UserAddress
import kotlin.concurrent.thread
import ru.senin.kotlin.net.server.ChatMessageListener
import ru.senin.kotlin.net.server.HttpChatServer
import kotlin.test.*
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart

class ChatTest {
    val host = "0.0.0.0"
    val port = 8080
    val testUserName = "pupkin"
    val testText = "Hi there"

    @Test
    fun sendMessage() {
        val server = HttpChatServer(host, port)
        val client = HttpChatClient(host, port)
        server.setMessageListener(object: ChatMessageListener {
            override fun messageReceived(userName: String, text: String) {
                assertEquals(testUserName, userName)
                assertEquals(testText, text)
                thread {
                    Thread.sleep(1000);
                    server.stop()
                }
            }
        })
        val serverJob = thread {
            server.start()
        }
        client.sendMessage(Message(testUserName, testText))
    }
}
