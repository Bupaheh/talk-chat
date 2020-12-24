package ru.senin.kotlin.net.registry

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.UserAddress
import ru.senin.kotlin.net.UserInfo
import kotlin.test.*

fun Application.testModule() {

    (environment.config as MapApplicationConfig).apply {
        // define test environment here
    }
    module(testing = true)
}

class ApplicationTest {
    private val objectMapper = jacksonObjectMapper()
    private val testUserName = "pupkin"
    private val testHttpAddress = UserAddress(Protocol.HTTP, "127.0.0.1", 9999)
    private val testWebSocketAddress = UserAddress(Protocol.WEBSOCKET, "127.0.0.1", 8087)
    private val testUDPAddress = UserAddress(Protocol.UDP, "127.0.0.1", 8091)
    private val userData = UserInfo(testUserName, testHttpAddress)

    @BeforeEach
    fun clearRegistry() {
        Registry.users.clear()
    }

    @Test
    fun `health endpoint`() {
        withTestApplication({ testModule() }) {
            handleRequest(HttpMethod.Get, "/v1/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK", response.content)
            }
        }
    }

    @Test
    fun `register user`() = withRegisteredTestUser(testHttpAddress) {

        //test with incorrect name
        val testUserData = UserInfo("Test test", testHttpAddress)
        handleRequest {
            method = HttpMethod.Post
            uri = "/v1/users"
            addHeader("Content-type", "application/json")
            setBody(objectMapper.writeValueAsString(testUserData))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val content = response.content ?: fail("No response content")
            assertEquals("Illegal user name", content)
        }

        //user already registered test
        handleRequest {
            method = HttpMethod.Post
            uri = "/v1/users"
            addHeader("Content-type", "application/json")
            setBody(objectMapper.writeValueAsString(userData))
        }.apply {
            assertEquals(HttpStatusCode.Conflict, response.status())
            val content = response.content ?: fail("No response content")
            assertEquals("User already registered", content)
        }
    }

    @Test
    fun `list HTTP user`() = withRegisteredTestUser(testHttpAddress) {
        handleRequest(HttpMethod.Get, "/v1/users").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val content = response.content ?: fail("No response content")
            val users = objectMapper.readValue<HashMap<String, UserAddress>>(content)
            assertEquals(users[testUserName], testHttpAddress)
        }
    }

    @Test
    fun `list WebSocket user`() = withRegisteredTestUser(testWebSocketAddress) {
        handleRequest(HttpMethod.Get, "/v1/users").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val content = response.content ?: fail("No response content")
            val users = objectMapper.readValue<HashMap<String, UserAddress>>(content)
            assertEquals(users[testUserName], testWebSocketAddress)
        }
    }

    @Test
    fun `list UDP user`() = withRegisteredTestUser(testUDPAddress) {
        handleRequest(HttpMethod.Get, "/v1/users").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val content = response.content ?: fail("No response content")
            val users = objectMapper.readValue<HashMap<String, UserAddress>>(content)
            assertEquals(users[testUserName], testUDPAddress)
        }
    }

    @Test
    fun `list users with empty user list`() {
        withTestApplication({ testModule() }) {
            handleRequest(HttpMethod.Get, "/v1/users").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val users = objectMapper.readValue<HashMap<String, UserAddress>>(content)
                assertEquals(0, users.size)
            }
        }
    }


    @Test
    fun `delete user`() = withRegisteredTestUser(testHttpAddress) {
        handleRequest {
            method = HttpMethod.Delete
            uri = "/v1/users/${testUserName}"
            addHeader("Content-type", "application/json")
        }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val content = response.content ?: fail("No response content")
            val info = objectMapper.readValue<HashMap<String,String>>(content)

            assertNotNull(info["status"])
            assertEquals("ok", info["status"])
        }
        handleRequest(HttpMethod.Get, "/v1/users").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val content = response.content ?: fail("No response content")
            val users = objectMapper.readValue<HashMap<String, UserAddress>>(content)
            assertFalse(testUserName in users)
        }
    }

    @Test
    suspend fun `HTTP healthCheck`() {
        assertFalse(checkHealth(testHttpAddress))
    }

    @Test
    suspend fun `WebSocket healthCheck`() {
        assertFalse(checkHealth(testWebSocketAddress))
    }

    private fun withRegisteredTestUser(testAddress: UserAddress, block: TestApplicationEngine.() -> Unit) {
        withTestApplication({ testModule() }) {
            handleRequest {
                method = HttpMethod.Post
                uri = "/v1/users"
                addHeader("Content-type", "application/json")
                val testUserData = UserInfo(testUserName, testAddress)
                setBody(objectMapper.writeValueAsString(testUserData))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val content = response.content ?: fail("No response content")
                val info = objectMapper.readValue<HashMap<String,String>>(content)

                assertNotNull(info["status"])
                assertEquals("ok", info["status"])

                this@withTestApplication.block()
            }
        }
    }
}
