package ru.senin.kotlin.net.registry

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.netty.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.event.Level
import ru.senin.kotlin.net.UserAddress
import ru.senin.kotlin.net.UserInfo
import ru.senin.kotlin.net.checkUserName
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.xml.bind.JAXBElement
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    val client = HttpClient()
    GlobalScope.launch {
        // TODO: periodically check users and remove unreachable ones
        while (true) {
            val it = Registry.users.iterator()
            println(Registry.users)
            while (it.hasNext()) {
                val userAddress = it.next().value;
                val call: String? = try { client.get("$userAddress/v1/health") } catch (e: Exception) { null }
                println(call)
                if (call != "OK")
                    it.remove();
            }
            delay(2000);
        }
    }
    EngineMain.main(args)
}

object Registry {
    val users = ConcurrentHashMap<String, UserAddress>()
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
            if (Registry.users.contains(name)) {
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
            Registry.users[name] = call.receive<UserAddress>()
            call.respond(mapOf("status" to "ok"))
        }

        delete("v1/users/{name}") {
            val name = call.parameters["name"] ?: ""
            Registry.users.remove(name)
            call.respond(mapOf("status" to "ok"))
        }
    }
}

class UserAlreadyRegisteredException: RuntimeException("User already registered")
class IllegalUserNameException: RuntimeException("Illegal user name")