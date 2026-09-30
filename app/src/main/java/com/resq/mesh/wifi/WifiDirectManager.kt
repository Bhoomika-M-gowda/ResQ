package com.resq.mesh.wifi

import android.annotation.SuppressLint
import android.content.*
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import androidx.core.content.ContextCompat
import com.resq.data.db.SupportDao
import com.resq.data.model.EmergencyPacket
import com.resq.data.model.ForwardingLog
import com.resq.data.model.PacketStatus
import com.resq.data.repository.EmergencyRepository
import com.resq.mesh.packet.MeshAck
import com.resq.mesh.packet.MeshProtocol
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

data class WifiPeer(val name: String, val address: String)

data class WifiMeshState(
    val available: Boolean = true,
    val enabled: Boolean = false,
    val discovering: Boolean = false,
    val hosting: Boolean = false,
    val connected: Boolean = false,
    val peers: List<WifiPeer> = emptyList(),
    val status: String = "Wi-Fi Direct not started",
    val error: String? = null,
    val receivedPacketId: String? = null
)

class WifiDirectManager(
    context: Context,
    private val emergencyRepository: EmergencyRepository,
    private val supportDao: SupportDao,
    private val localDeviceId: String,
    private val isRescueMode: () -> Boolean
) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(WifiP2pManager::class.java)
    private val channel = manager?.initialize(appContext, appContext.mainLooper, null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(WifiMeshState(available = manager != null && channel != null))
    val state: StateFlow<WifiMeshState> = _state.asStateFlow()
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var pendingTransfer: Pair<WifiPeer, EmergencyPacket>? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val enabled = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    _state.value = _state.value.copy(enabled = enabled, status = if (enabled) "Wi-Fi Direct ready" else "Wi-Fi Direct unavailable")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> requestPeers()
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> requestConnectionInfo()
                WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                    val active = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1) == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED
                    _state.value = _state.value.copy(discovering = active)
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        val p2p = manager ?: return
        val ch = channel ?: return
        p2p.discoverPeers(ch, action(
            success = { _state.value = _state.value.copy(discovering = true, status = "Discovering Wi-Fi Direct peers…", error = null) },
            failure = { _state.value = _state.value.copy(error = "Discovery failed: reason $it") }
        ))
    }

    @SuppressLint("MissingPermission")
    fun startHost() {
        val p2p = manager ?: return
        val ch = channel ?: return
        fun create() = p2p.createGroup(ch, action(
            success = {
                _state.value = _state.value.copy(hosting = true, status = "Wi-Fi Direct host ready; waiting for sender", error = null)
                requestConnectionInfo()
            },
            failure = { _state.value = _state.value.copy(error = "Could not create receiver group: reason $it") }
        ))
        p2p.removeGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = create()
            override fun onFailure(reason: Int) = create()
        })
    }

    @SuppressLint("MissingPermission")
    fun connectAndSend(peer: WifiPeer, packet: EmergencyPacket) {
        val p2p = manager ?: return
        val ch = channel ?: return
        pendingTransfer = peer to packet
        val config = WifiP2pConfig().apply {
            deviceAddress = peer.address
            wps.setup = WpsInfo.PBC
        }
        _state.value = _state.value.copy(status = "Connecting to ${peer.name}…", error = null)
        p2p.connect(ch, config, action(
            success = { _state.value = _state.value.copy(status = "Wi-Fi Direct negotiation started") },
            failure = {
                pendingTransfer = null
                _state.value = _state.value.copy(error = "Connection failed: reason $it", status = "Packet remains stored")
            }
        ))
    }

    @SuppressLint("MissingPermission")
    private fun requestPeers() {
        val p2p = manager ?: return
        val ch = channel ?: return
        p2p.requestPeers(ch) { list ->
            val peers = list.deviceList.map {
                WifiPeer(it.deviceName?.ifBlank { "Wi-Fi Direct device" } ?: "Wi-Fi Direct device", it.deviceAddress)
            }
                .sortedBy { it.name.lowercase() }
            _state.value = _state.value.copy(peers = peers, discovering = false, status = "${peers.size} Wi-Fi peer(s) found")
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestConnectionInfo() {
        val p2p = manager ?: return
        val ch = channel ?: return
        p2p.requestConnectionInfo(ch) { info ->
            if (!info.groupFormed) return@requestConnectionInfo
            _state.value = _state.value.copy(connected = true, hosting = info.isGroupOwner, status = if (info.isGroupOwner) "Hosting Wi-Fi receiver" else "Connected to Wi-Fi receiver")
            if (info.isGroupOwner) startTcpServer()
            else pendingTransfer?.let { (peer, packet) ->
                pendingTransfer = null
                scope.launch { sendToHost(info.groupOwnerAddress.hostAddress, peer, packet) }
            }
        }
    }

    private fun startTcpServer() {
        if (serverJob?.isActive == true) return
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                _state.value = _state.value.copy(hosting = true, status = "Wi-Fi receiver listening on port $PORT", error = null)
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    launch { receive(socket) }
                }
            } catch (error: Exception) {
                if (isActive) _state.value = _state.value.copy(error = error.message ?: "Wi-Fi receiver stopped")
            }
        }
    }

    private suspend fun sendToHost(host: String, peer: WifiPeer, packet: EmergencyPacket) {
        val now = System.currentTimeMillis()
        val outbound = packet.copy(hopCount = packet.hopCount + 1, status = PacketStatus.FORWARDED, lastForwardedAt = now)
        val result = runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, PORT), CONNECT_TIMEOUT_MS)
                socket.soTimeout = ACK_TIMEOUT_MS
                val writer = socket.outputStream.bufferedWriter()
                writer.write(MeshProtocol.encodePacket(outbound, localDeviceId)); writer.newLine(); writer.flush()
                val line = socket.inputStream.bufferedReader().readLine() ?: error("No Wi-Fi acknowledgement")
                val ack = MeshProtocol.decodeAck(line).getOrThrow()
                require(ack.messageId == packet.messageId) { "Acknowledgement ID mismatch" }
                require(ack.accepted) { ack.reason ?: "Receiver rejected packet" }
                ack
            }
        }
        result.fold(
            onSuccess = { ack ->
                emergencyRepository.markTransferred(packet.messageId, outbound.hopCount, ack.finalDelivery)
                supportDao.insertForwardingLog(ForwardingLog(messageId = packet.messageId, fromDevice = localDeviceId, toDevice = ack.receiverId, method = "WIFI_LOCAL", timestamp = now, result = if (ack.finalDelivery) "ACK_DELIVERED" else "ACK_FORWARDED"))
                _state.value = _state.value.copy(status = if (ack.finalDelivery) "${packet.messageId} delivered to Rescue by Wi-Fi" else "${packet.messageId} acknowledged by ${peer.name}", error = null)
            },
            onFailure = { error ->
                supportDao.insertForwardingLog(ForwardingLog(messageId = packet.messageId, fromDevice = localDeviceId, toDevice = peer.address, method = "WIFI_LOCAL", timestamp = now, result = "FAILED"))
                _state.value = _state.value.copy(error = error.message ?: "Wi-Fi transfer failed", status = "Packet remains stored")
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun broadcastPacket(packet: EmergencyPacket) {
        discoverPeers()
        val currentPeers = _state.value.peers
        if (currentPeers.isNotEmpty()) {
            currentPeers.forEach { peer ->
                connectAndSend(peer, packet)
            }
        }
    }

    private suspend fun receive(socket: Socket) {
        socket.use { connected ->
            connected.soTimeout = ACK_TIMEOUT_MS
            val line = runCatching { connected.inputStream.bufferedReader().readLine() }.getOrNull()
            if (line.isNullOrBlank() || line.length > MAX_PACKET_CHARS) {
                sendAck(connected, MeshAck("UNKNOWN", false, false, localDeviceId, "Malformed packet")); return
            }
            val message = MeshProtocol.decodePacket(line).getOrElse {
                sendAck(connected, MeshAck("UNKNOWN", false, false, localDeviceId, "Malformed mesh packet")); return
            }
            val finalDelivery = isRescueMode()
            if (message.packet.priority == com.resq.data.model.EmergencyPriority.CRITICAL || message.packet.type == com.resq.data.model.EmergencyType.SOS) {
                com.resq.util.SosNotificationManager.showSosReceivedNotification(
                    appContext,
                    message.forwarderId,
                    message.packet.text,
                    message.packet.latitude,
                    message.packet.longitude
                )
            }
            emergencyRepository.receivePacket(message.packet, finalDelivery).fold(
                onSuccess = { stored ->
                    sendAck(connected, MeshAck(stored.messageId, true, finalDelivery, localDeviceId))
                    supportDao.insertForwardingLog(ForwardingLog(messageId = stored.messageId, fromDevice = message.forwarderId, toDevice = localDeviceId, method = "WIFI_LOCAL", timestamp = System.currentTimeMillis(), result = if (finalDelivery) "RESCUE_RECEIVED" else "RECEIVED"))
                    _state.value = _state.value.copy(status = if (finalDelivery) "Rescue received ${stored.messageId} by Wi-Fi" else "Received ${stored.messageId} by Wi-Fi", receivedPacketId = stored.messageId, error = null)
                },
                onFailure = { error ->
                    sendAck(connected, MeshAck(message.packet.messageId, false, finalDelivery, localDeviceId, error.message))
                    _state.value = _state.value.copy(error = error.message ?: "Incoming packet rejected")
                }
            )
        }
    }

    private fun sendAck(socket: Socket, ack: MeshAck) {
        runCatching {
            val writer = socket.outputStream.bufferedWriter()
            writer.write(MeshProtocol.encodeAck(ack)); writer.newLine(); writer.flush()
        }
    }

    fun close() {
        runCatching { serverSocket?.close() }
        serverJob?.cancel()
        scope.cancel()
        runCatching { appContext.unregisterReceiver(receiver) }
    }

    private fun action(success: () -> Unit, failure: (Int) -> Unit) = object : WifiP2pManager.ActionListener {
        override fun onSuccess() = success()
        override fun onFailure(reason: Int) = failure(reason)
    }

    companion object {
        private const val PORT = 8988
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val ACK_TIMEOUT_MS = 15_000
        private const val MAX_PACKET_CHARS = 8_192
    }
}
