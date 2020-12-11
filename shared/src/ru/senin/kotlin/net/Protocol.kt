package ru.senin.kotlin.net

enum class Protocol {
    HTTP, WEBSOCKET, UDP
}

data class UserAddress(
    val protocol: Protocol,
    val host: String,
    val port: Int = 8080
) {
    override fun toString(): String {
        return "http://${host}:${port}"
    }
}

data class UserInfo(val name: String, val address: UserAddress)

data class Message(val user: String, val text: String)

data class UdpHealthCheckData(val host: String, val port: Int, val id: String)

//"healthCheck" is reserved for UDP health check
fun checkUserName(name: String) = if (name == "healthCheck") null else """^[a-zA-Z0-9-_.]+$""".toRegex().find(name)
