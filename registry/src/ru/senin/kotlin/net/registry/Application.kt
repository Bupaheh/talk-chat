package ru.senin.kotlin.net.registry

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.jackson.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.event.Level
import ru.senin.kotlin.net.*
import java.lang.Exception
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.random.Random.Default.nextLong

val client = HttpClient {
    install(WebSockets)
}

suspend fun checkHealth(userAddress: UserAddress): Boolean =
    try {
        when (userAddress.protocol) {
            Protocol.HTTP -> {
                val call: String = client.get("$userAddress/v1/health")
                call == "OK"
            }
            Protocol.WEBSOCKET -> {
                client.ws(host = userAddress.host, port = userAddress.port) {
                }
                true
            }
            Protocol.UDP -> {
                val result = Registry.healthCheckServer.getHealthCheckStatus(userAddress)
                val objectMapper = jacksonObjectMapper()
                val socket = aSocket(ActorSelectorManager(Dispatchers.IO))
                    .udp().connect(InetSocketAddress(userAddress.host, userAddress.port))
                val output = socket.openWriteChannel(autoFlush = true)
                val id = nextLong().toString()
                val data = objectMapper.writeValueAsString(UdpHealthCheckData("0.0.0.0", 8088, id))
                Registry.healthCheckServer.updatePendingUpdRequests(userAddress, id)
                output.writeStringUtf8(objectMapper.writeValueAsString(Message("healthCheck", data)))
                result
            }
        }
    }
    catch (e: Exception) {
        false
    }

fun main(args: Array<String>) {
    thread(isDaemon = true) {
        Registry.healthCheckServer.start()
    }
    GlobalScope.launch {
        val failedChecks = mutableMapOf<String, Int>()
        while (true) {
            for ((user, userAddress) in Registry.users) {
                if (checkHealth(userAddress))
                    failedChecks.remove(user)
                else
                    failedChecks[user] = failedChecks.getOrDefault(user, 0) + 1
            }
            val usersToRemove = failedChecks.filter { it.value > 3 }.map { it.key }
            usersToRemove.forEach { Registry.users.remove(it) }
            failedChecks -= failedChecks.keys.filterNot { Registry.users.containsKey(it) }
            delay(1 * 1000)
        }
    }
    EngineMain.main(args)
}

object Registry {
    val users = ConcurrentHashMap<String, UserAddress>()
    val healthCheckServer = UdpHealthCheckServer("0.0.0.0", 8088)
}

@Suppress("UNUSED_PARAMETER")
@JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "invalid argument")
        }
        exception<UserAlreadyRegisteredException> { cause ->
            call.respond(HttpStatusCode.Conflict, cause.message ?: "user already registered")
        }
        exception<IllegalUserNameException> { cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "illegal user name")
        }
    }
    routing {
        get("/v1/health") {
            call.respondText("OK", contentType = ContentType.Text.Plain)
        }

        post("/v1/users") {
            val user = call.receive<UserInfo>()
            val name = user.name
            checkUserName(name) ?: throw IllegalUserNameException()
            if (Registry.users.containsKey(name)) {
                throw UserAlreadyRegisteredException()
            }
            Registry.users[name] = user.address
            call.respond(mapOf("status" to "ok"))
        }

        get("/v1/users") {
            call.respond(Registry.users)
        }

        put("/v1/users/{name}") {
            val name = call.parameters["name"] ?: ""
            checkUserName(name) ?: throw IllegalUserNameException()
            Registry.users[name] = call.receive()
            call.respond(mapOf("status" to "ok"))
        }

        delete("v1/users/{name}") {
            val name = call.parameters["name"] ?: ""
            Registry.users.remove(name)
            call.respond(mapOf("status" to "ok"))
        }
    }
}

class UserAlreadyRegisteredException : RuntimeException("User already registered")
class IllegalUserNameException : RuntimeException("Illegal user name")