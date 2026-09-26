package com.example.network.nearby

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.data.Message
import com.example.security.EncryptionHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

class P2PManager(
    private val context: Context,
    private val myUserId: String,
    private val onMessageReceived: (Message) -> Unit
) {
    private val tag = "P2PManager"
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val prefs = context.getSharedPreferences("ai_messenger_prefs", Context.MODE_PRIVATE)
    val deviceId: String = prefs.getString("device_id", null) ?: run {
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", newId).apply()
        newId
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val SERVICE_TYPE = "_aimessenger._tcp."
    private val localServiceName = "AIMessenger-${deviceId.take(6)}"

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val dtoAdapter = moshi.adapter(P2PMessageDto::class.java)

    private val _isMeshEnabled = MutableStateFlow(false)
    val isMeshEnabled: StateFlow<Boolean> = _isMeshEnabled

    private val _connectionStatus = MutableStateFlow("Offline")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _discoveredPeers = MutableStateFlow<List<Peer>>(emptyList())
    val discoveredPeers: StateFlow<List<Peer>> = _discoveredPeers

    data class Peer(
        val id: String,
        val name: String,
        val ipAddress: String,
        val port: Int,
        val isOnline: Boolean
    )

    fun setMeshEnabled(enabled: Boolean) {
        if (_isMeshEnabled.value == enabled) return
        _isMeshEnabled.value = enabled
        if (enabled) {
            startServerAndDiscovery()
        } else {
            stopServerAndDiscovery()
        }
    }

    private fun startServerAndDiscovery() {
        isRunning = true
        _connectionStatus.value = "Starting Nearby LAN..."
        scope.launch {
            try {
                // Bind to ephemeral available port
                serverSocket = ServerSocket(0)
                val assignedPort = serverSocket?.localPort ?: 8888
                Log.d(tag, "P2P server socket bound on port $assignedPort")

                registerNsdService(assignedPort)
                startNsdDiscovery()

                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    handleIncomingConnection(socket)
                }
            } catch (e: Exception) {
                Log.e(tag, "P2P server error: ${e.message}")
                _connectionStatus.value = "Server Error: ${e.message}"
            } finally {
                _connectionStatus.value = "Offline"
            }
        }
    }

    private fun registerNsdService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = localServiceName
            serviceType = SERVICE_TYPE
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(tag, "NSD Service registered: ${info.serviceName}")
                _connectionStatus.value = "Discovering Nearby Devices..."
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "NSD Registration failed: $errorCode")
                _connectionStatus.value = "Registration Failed ($errorCode)"
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d(tag, "NSD Service unregistered: ${info.serviceName}")
            }
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(tag, "NSD Unregistration failed: $errorCode")
            }
        }

        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun startNsdDiscovery() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(tag, "NSD Discovery started")
            }
            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(tag, "NSD Service found: ${service.serviceName} (${service.serviceType})")
                if (service.serviceType.contains("_aimessenger")) {
                    if (service.serviceName != localServiceName) {
                        try {
                            nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                    Log.e(tag, "NSD Resolve failed: $errorCode")
                                }
                                override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                                    Log.d(tag, "NSD Service resolved: $resolvedService")
                                    val host = resolvedService.host?.hostAddress ?: return
                                    val port = resolvedService.port
                                    val peerName = resolvedService.serviceName ?: "Nearby Peer"
                                    val peerId = resolvedService.serviceName

                                    val currentList = _discoveredPeers.value.toMutableList()
                                    val existingIndex = currentList.indexOfFirst { it.name == peerName || it.ipAddress == host }
                                    val peer = Peer(
                                        id = peerId,
                                        name = peerName,
                                        ipAddress = host,
                                        port = port,
                                        isOnline = true
                                    )
                                    if (existingIndex >= 0) {
                                        currentList[existingIndex] = peer
                                    } else {
                                        currentList.add(peer)
                                    }
                                    _discoveredPeers.value = currentList
                                    MeshRouter.registerPeer(peerId, host)
                                    _connectionStatus.value = "Connected / Discovered (${currentList.size} peers)"
                                }
                            })
                        } catch (e: Exception) {
                            Log.e(tag, "Error initiating service resolve: ${e.message}")
                        }
                    }
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(tag, "NSD Service lost: ${service.serviceName}")
                val currentList = _discoveredPeers.value.toMutableList()
                currentList.removeIf { it.name == service.serviceName }
                _discoveredPeers.value = currentList
                if (currentList.isEmpty()) {
                    _connectionStatus.value = "Discovering Nearby Devices (No peers)"
                }
            }
            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(tag, "NSD Discovery stopped: $serviceType")
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "NSD Discovery start failed: $errorCode")
                _connectionStatus.value = "Discovery Failed ($errorCode)"
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "NSD Discovery stop failed: $errorCode")
            }
        }

        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun stopServerAndDiscovery() {
        isRunning = false
        try {
            registrationListener?.let {
                nsdManager?.unregisterService(it)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering NSD service: ${e.message}")
        }
        registrationListener = null

        try {
            discoveryListener?.let {
                nsdManager?.stopServiceDiscovery(it)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping NSD discovery: ${e.message}")
        }
        discoveryListener = null

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing server socket: ${e.message}")
        }
        serverSocket = null
        _connectionStatus.value = "Offline"
        _discoveredPeers.value = emptyList()
    }

    private fun handleIncomingConnection(socket: Socket) {
        scope.launch {
            try {
                socket.soTimeout = 10_000 // 10s read timeout
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = reader.readLine() ?: return@launch
                Log.d(tag, "Incoming JSON payload received")

                val dto = dtoAdapter.fromJson(line) ?: run {
                    Log.w(tag, "Failed to parse incoming JSON payload")
                    return@launch
                }

                // Strict validation
                if (dto.protocolVersion != 1) {
                    Log.w(tag, "Unsupported protocol version: ${dto.protocolVersion}")
                    return@launch
                }
                if (dto.messageId.isBlank() || dto.senderDeviceId.isBlank() || dto.conversationId.isBlank() || dto.encryptedPayload.isBlank()) {
                    Log.w(tag, "Missing required DTO fields")
                    return@launch
                }
                if (!MeshRouter.shouldForward(dto.ttl)) {
                    Log.w(tag, "Message TTL expired / exceeded hop limit")
                    return@launch
                }
                if (!MeshRouter.registerMessageAndCheckDuplicate(dto.messageId)) {
                    return@launch
                }

                // Decrypt payload with AES-GCM (fails closed if tampered or wrong key)
                val secretKey = EncryptionHelper.deriveKey("AI_MESSENGER_MESH_KEY")
                val decryptedText = EncryptionHelper.decrypt(dto.encryptedPayload, secretKey)

                val message = Message(
                    id = dto.messageId,
                    conversationId = dto.conversationId,
                    senderId = dto.senderDeviceId,
                    senderName = dto.senderName,
                    text = decryptedText,
                    timestamp = dto.timestamp,
                    status = "READ",
                    isIncoming = true,
                    transportUsed = "NEARBY",
                    attachmentType = if (dto.messageType == "TEXT") null else dto.messageType,
                    attachmentPath = dto.attachmentMetadata
                )
                onMessageReceived(message)
            } catch (e: Exception) {
                Log.e(tag, "Error processing incoming connection payload: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun sendMessageToPeer(ipAddress: String, port: Int, message: Message): Boolean = withContext(Dispatchers.IO) {
        if (!isMeshEnabled.value) return@withContext false
        var socket: Socket? = null
        try {
            socket = Socket().apply {
                connect(java.net.InetSocketAddress(ipAddress, port), 5000) // 5s connect timeout
                soTimeout = 5000
            }
            val writer = PrintWriter(socket.getOutputStream(), true)

            val secretKey = EncryptionHelper.deriveKey("AI_MESSENGER_MESH_KEY")
            val cipherText = EncryptionHelper.encrypt(message.text, secretKey)

            val dto = P2PMessageDto(
                protocolVersion = 1,
                messageId = message.id,
                senderDeviceId = deviceId,
                senderName = message.senderName,
                conversationId = message.conversationId,
                timestamp = message.timestamp,
                ttl = 3,
                messageType = message.attachmentType ?: "TEXT",
                encryptedPayload = cipherText,
                attachmentMetadata = message.attachmentPath
            )

            val jsonPayload = dtoAdapter.toJson(dto)
            writer.println(jsonPayload)
            Log.d(tag, "P2P JSON message successfully sent to $ipAddress:$port")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to send message to $ipAddress:$port: ${e.message}")
            false
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }
}
