package com.example.network.nearby

import android.util.Log
import com.example.data.Message
import com.example.domain.CommunicationTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NearbyTransport(private val p2pManager: P2PManager) : CommunicationTransport {
    private val tag = "NearbyTransport"

    override val name: String = "Nearby Mesh"

    override fun isAvailable(): Boolean {
        // Available if mesh settings are enabled by the user and there are discovered peers
        return p2pManager.isMeshEnabled.value && p2pManager.discoveredPeers.value.isNotEmpty()
    }

    override suspend fun sendMessage(message: Message): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) {
            Log.d(tag, "Nearby Mesh transport is not available.")
            return@withContext false
        }

        val peers = p2pManager.discoveredPeers.value
        var anySuccess = false

        // Propagate / multicast message to all nearby discovered peers (gossiping/flooding style)
        for (peer in peers) {
            if (peer.isOnline) {
                MeshRouter.addLog("Forwarding message ${message.id} to peer ${peer.name} (${peer.ipAddress})")
                val success = p2pManager.sendMessageToPeer(peer.ipAddress, message)
                if (success) {
                    anySuccess = true
                }
            }
        }
        anySuccess
    }
}
