package com.example.network.nearby

import com.example.data.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

object MeshRouter {
    private const val MAX_HOP_LIMIT = 5
    private const val ROUTE_EXPIRATION_MS = 600_000 // 10 minutes

    // Replay protection: Store received message IDs and their timestamp
    private val processedMessageIds = ConcurrentHashMap<String, Long>()

    // Active routing table: peerId -> IP address
    private val routingTable = ConcurrentHashMap<String, RouteInfo>()

    data class RouteInfo(val ipAddress: String, val lastSeen: Long)

    // Rate limiter: count of messages sent per peer
    private val messageCounts = ConcurrentHashMap<String, Int>()

    private val _meshLogs = MutableStateFlow<List<String>>(emptyList())
    val meshLogs: StateFlow<List<String>> = _meshLogs

    fun addLog(log: String) {
        val current = _meshLogs.value.toMutableList()
        current.add(0, "[Mesh] $log")
        if (current.size > 50) current.removeAt(current.size - 1)
        _meshLogs.value = current
    }

    // Returns true if the message is new and should be processed
    fun registerMessageAndCheckDuplicate(messageId: String): Boolean {
        cleanupExpiredMessages()
        val now = System.currentTimeMillis()
        if (processedMessageIds.containsKey(messageId)) {
            addLog("Duplicate message $messageId detected. Dropping for replay protection.")
            return false
        }
        processedMessageIds[messageId] = now
        return true
    }

    fun registerPeer(peerId: String, ipAddress: String) {
        routingTable[peerId] = RouteInfo(ipAddress, System.currentTimeMillis())
        addLog("Registered route to peer $peerId at $ipAddress")
    }

    fun getRouteForPeer(peerId: String): String? {
        val route = routingTable[peerId] ?: return null
        if (System.currentTimeMillis() - route.lastSeen > ROUTE_EXPIRATION_MS) {
            routingTable.remove(peerId)
            addLog("Route to $peerId expired.")
            return null
        }
        return route.ipAddress
    }

    // Decrement TTL (Time to live) / hop limit for forwarding
    fun shouldForward(hopCount: Int): Boolean {
        return hopCount < MAX_HOP_LIMIT
    }

    private fun cleanupExpiredMessages() {
        val now = System.currentTimeMillis()
        val iterator = processedMessageIds.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > ROUTE_EXPIRATION_MS) {
                iterator.remove()
            }
        }
    }
}
