package ru.senin.kotlin.net.registry

import com.fasterxml.jackson.module.kotlin.readValue
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.UdpHealthCheckData
import ru.senin.kotlin.net.UserAddress
import ru.senin.kotlin.net.server.UdpChatServer
import java.util.concurrent.ConcurrentHashMap

class UdpHealthCheckServer(host: String, port: Int): UdpChatServer(host, port) {
    private val pendingUdpRequests = ConcurrentHashMap<UserAddress, String>()

    fun getHealthCheckStatus(address: UserAddress) = pendingUdpRequests.getOrDefault(address, "OK") == "OK"

    fun updatePendingUpdRequests(address: UserAddress, id: String) {
        pendingUdpRequests[address] = id
    }

    override suspend fun healthCheck(text: String) {
        val data = objectMapper.readValue<UdpHealthCheckData>(text)
        val address = UserAddress(Protocol.UDP, data.host, data.port)
        if (pendingUdpRequests.containsKey(address) && pendingUdpRequests[address] == data.id)
            pendingUdpRequests[address] = "OK"
    }
}