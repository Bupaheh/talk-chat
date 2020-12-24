package ru.senin.kotlin.net

import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.argument
import com.apurebase.arkenv.parse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.senin.kotlin.net.server.ChatServer
import java.net.URL
import kotlin.concurrent.thread

class Parameters : Arkenv() {
    val name : String by argument("--name") {
        description = "Name of user"
    }

    val registryBaseUrl : String by argument("--registry"){
        description = "Base URL of User Registry"
        defaultValue = { "http://localhost:8088" }
    }

    val host : String by argument("--host"){
        description = "Hostname or IP to listen on"
        defaultValue = { "0.0.0.0" } // 0.0.0.0 - listen on all network interfaces
    }

    val port : Int? by argument("--port") {
        description = "Port to listen for on"
    }

    val publicUrl : String? by argument("--public-url") {
        description = "Public URL"
    }

    val protocolName : String by argument("--protocol") {
        description = "Server protocol"
        defaultValue = { Protocol.HTTP.name }
    }
}

val log: Logger = LoggerFactory.getLogger("main")
lateinit var parameters : Parameters

fun checkHost(host: String) = host.length <= 253 && Regex("""(\p{Alnum}{1,63}.)*\p{Alnum}{1,63}""").matches(host)

@ExperimentalCoroutinesApi
fun main(args: Array<String>) {
    try {
        parameters = Parameters().parse(args)

        if (parameters.help) {
            println(parameters.toString())
            return
        }
        val protocol = Protocol.valueOf(parameters.protocolName)
        val host = parameters.host
        val port = parameters.port ?: protocol.defaultPort

        // TODO: validate host and port
        if (!checkHost(host))
            throw IllegalArgumentException("Illegal host '$host'")
        if (port !in 1..65535)
            throw IllegalArgumentException("Illegal port $port")

        val name = parameters.name
        checkUserName(name) ?: throw IllegalArgumentException("Illegal user name '$name'")

        // initialize registry interface
        val objectMapper = jacksonObjectMapper()
        val registry = Retrofit.Builder()
            .baseUrl(parameters.registryBaseUrl)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build().create(RegistryApi::class.java)

        // create server engine
        val server = ChatServer.create(protocol, host, port)
        val chat = Chat(name, registry)
        server.setMessageListener(chat)

        // start server as separate job
        val serverJob = thread {
            server.start()
        }
        try {
            // register our client
            val userAddress  = when {
                parameters.publicUrl != null -> {
                    val url = URL(parameters.publicUrl)
                    if (url.port == -1)
                        UserAddress(protocol, url.host, protocol.defaultUrlPort)
                    else
                        UserAddress(protocol, url.host, url.port)
                }
                else -> UserAddress(protocol, host, port)
            }
            registry.register(UserInfo(name, userAddress)).execute()

            // start
            chat.commandLoop()
        }
        finally {
            registry.unregister(name).execute()
            server.stop()
            serverJob.join()
        }
    }
    catch (e: Exception) {
        log.error("Error! ${e.message}", e)
        println("Error! ${e.message}")
    }
}
