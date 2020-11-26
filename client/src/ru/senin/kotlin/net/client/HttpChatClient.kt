package ru.senin.kotlin.net.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ru.senin.kotlin.net.HttpApi
import ru.senin.kotlin.net.Message

class HttpChatClient(host: String, port: Int) {
    private val objectMapper = jacksonObjectMapper()
    private val httpApi: HttpApi = TODO("Create HttpApi Retrofit implementation with base url http://$host:$port")

    fun sendMessage(message: Message) {
        val response = httpApi.sendMessage(message).execute()
        if (!response.isSuccessful) {
            println("${response.code()} ${response.message()}}")
        }
    }
}
