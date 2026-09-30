package com.resq.mesh.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.*
import android.os.Build
import androidx.core.content.ContextCompat
import com.resq.data.db.SupportDao
import com.resq.data.model.EmergencyPacket
import com.resq.data.model.ForwardingLog
import com.resq.data.repository.EmergencyRepository
import com.resq.mesh.packet.MeshAck
import com.resq.mesh.packet.MeshProtocol
import com.resq.mesh.packet.PacketValidator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PeerDevice(val name: String, val address: String, val paired: Boolean)

data class MeshUiState(
    val available: Boolean = true,
    val enabled: Boolean = false,
    val scanning: Boolean = false,
    val listening: Boolean = false,
    val peers: List<PeerDevice> = emptyList(),
    val status: String = "Bluetooth not started",
    val error: String? = null,
    val receivedPacketId: String? = null
)

class BluetoothMeshManager(
    context: Context,
    private val emergencyRepository: EmergencyRepository,
    private val supportDao: SupportDao,
    private val localDeviceId: String,
    private val isRescueMode: () -> Boolean
) {
    private val appContext = context.applicationContext
    private val adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(MeshUiState(available = adapter != null))
    val state: StateFlow<MeshUiState> = _state.asStateFlow()
    private var serverJob: Job? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> deviceFrom(intent)?.let(::addPeer)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _state.value = _state.value.copy(scanning = false, status = "Scan finished")
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> refresh()
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true
    }

    @SuppressLint("MissingPermission")
    fun refresh() {
        val bluetooth = adapter ?: run {
            _state.value = MeshUiState(available = false, status = "Bluetooth is not supported")
            return
        }
        val enabled = runCatching { bluetooth.isEnabled }.getOrDefault(false)
        val bonded = if (enabled) runCatching {
            bluetooth.bondedDevices.map { device ->
                PeerDevice(device.name ?: "Unknown device", device.address, true)
            }.sortedBy { it.name.lowercase() }
        }.getOrDefault(emptyList()) else emptyList()
        _state.value = _state.value.copy(
            enabled = enabled,
            peers = mergePeers(bonded, _state.value.peers),
            status = if (enabled) "Ready for Bluetooth mesh" else "Bluetooth is off",
            error = null
        )
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val bluetooth = adapter ?: return
        if (!bluetooth.isEnabled) {
            _state.value = _state.value.copy(error = "Turn on Bluetooth first")
            return
        }
        refresh()
        bluetooth.cancelDiscovery()
        val started = bluetooth.startDiscovery()
        _state.value = _state.value.copy(
            scanning = started,
            status = if (started) "Scanning for nearby phones…" else "Bluetooth scan could not start",
            error = if (started) null else "Check Nearby devices permission"
        )
    }

    @SuppressLint("MissingPermission")
    fun startServer() {
        val bluetooth = adapter ?: return
        if (!bluetooth.isEnabled || serverJob?.isActive == true) return
        serverJob = scope.launch {
            try {
                serverSocket = bluetooth.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                _state.value = _state.value.copy(listening = true, status = "Listening for emergency packets", error = null)
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    launch { receive(socket) }
                }
            } catch (error: Exception) {
                if (isActive) _state.value = _state.value.copy(listening = false, error = error.message ?: "Bluetooth receiver stopped")
            } finally {
                _state.value = _state.value.copy(listening = false)
            }
        }
    }

    @SuppressLint("MissingPermission")

    suspend fun sendPacketToPeer(packet: EmergencyPacket, peer: PeerDevice): Result<MeshAck> = withContext(Dispatchers.IO) {
        val bluetooth = adapter ?: return@withContext Result.failure(IllegalStateException("Bluetooth not available"))
        if (packet.status == com.resq.data.model.PacketStatus.DELIVERED ||
            packet.status == com.resq.data.model.PacketStatus.CLOSED ||
            packet.status == com.resq.data.model.PacketStatus.EXPIRED) {
            return@withContext Result.failure(IllegalStateException("Packet ${packet.messageId} is already ${packet.status.name}"))
        }
        if (packet.hopCount >= PacketValidator.MAX_HOPS && !isRescueMode()) {
            emergencyRepository.markExpired(packet.messageId, packet.hopCount)
            return@withContext Result.failure(IllegalStateException("Packet ${packet.messageId} expired (hop count ${packet.hopCount} >= MAX_HOPS ${PacketValidator.MAX_HOPS})"))
        }
        _state.value = _state.value.copy(status = "Sending ${packet.messageId} to ${peer.name}…", error = null)
        runCatching { bluetooth.cancelDiscovery() }
        val now = System.currentTimeMillis()
        val outbound = packet.copy(
            hopCount = packet.hopCount + 1,
            status = com.resq.data.model.PacketStatus.FORWARDED,
            lastForwardedAt = now
        )
        val result = runCatching {
            val device = bluetooth.getRemoteDevice(peer.address)
            device.createRfcommSocketToServiceRecord(SERVICE_UUID).use { socket ->
                socket.connect()
                val writer = socket.outputStream.bufferedWriter()
                writer.write(MeshProtocol.encodePacket(outbound, localDeviceId))
                writer.newLine()
                writer.flush()
                val timeoutJob = launch {
                    delay(ACK_TIMEOUT_MS)
                    runCatching { socket.close() }
                }
                val ackLine = try {
                    socket.inputStream.bufferedReader().readLine()
                        ?: error("Receiver closed without acknowledgement")
                } finally {
                    timeoutJob.cancel()
                }
                val ack = MeshProtocol.decodeAck(ackLine).getOrThrow()
                require(ack.messageId == packet.messageId) { "Acknowledgement ID mismatch" }
                require(ack.accepted) { ack.reason ?: "Receiver rejected packet" }
                ack
            }
        }
        result.fold(
            onSuccess = { ack ->
                emergencyRepository.markTransferred(packet.messageId, outbound.hopCount, ack.finalDelivery)
                if (ack.finalDelivery) {
                    emergencyRepository.markClosed(packet.messageId)
                }
                supportDao.insertForwardingLog(
                    ForwardingLog(
                        messageId = packet.messageId,
                        fromDevice = localDeviceId,
                        toDevice = ack.receiverId,
                        method = "BLUETOOTH",
                        timestamp = now,
                        result = if (ack.finalDelivery) "ACK_DELIVERED" else "ACK_FORWARDED"
                    )
                )
                _state.value = _state.value.copy(
                    status = if (ack.finalDelivery) "${packet.messageId} delivered to Rescue" else "${packet.messageId} acknowledged by ${peer.name}",
                    error = null
                )
                Result.success(ack)
            },
            onFailure = { error ->
                supportDao.insertForwardingLog(
                    ForwardingLog(messageId = packet.messageId, fromDevice = localDeviceId, toDevice = peer.address, method = "BLUETOOTH", timestamp = now, result = "FAILED")
                )
                _state.value = _state.value.copy(error = error.message ?: "Bluetooth send failed", status = "Packet remains stored")
                Result.failure(error)
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun send(packet: EmergencyPacket, peer: PeerDevice) {
        scope.launch { sendPacketToPeer(packet, peer) }
    }

    @SuppressLint("MissingPermission")
    fun broadcastPacket(packet: EmergencyPacket) {
        if (packet.status == com.resq.data.model.PacketStatus.DELIVERED ||
            packet.status == com.resq.data.model.PacketStatus.CLOSED ||
            packet.status == com.resq.data.model.PacketStatus.EXPIRED ||
            packet.hopCount >= PacketValidator.MAX_HOPS) {
            return
        }
        startDiscovery()
        val currentPeers = _state.value.peers
        if (currentPeers.isNotEmpty()) {
            currentPeers.forEach { peer ->
                send(packet, peer)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun receive(socket: BluetoothSocket) {
        val remote = runCatching { socket.remoteDevice.name ?: socket.remoteDevice.address }.getOrDefault("Nearby device")
        socket.use { connected ->
            val line = runCatching { connected.inputStream.bufferedReader().readLine() }.getOrElse {
                _state.value = _state.value.copy(error = it.message ?: "Could not read incoming packet")
                return
            }
            if (line.isNullOrBlank() || line.length > MAX_PACKET_CHARS) {
                sendAck(connected, MeshAck("UNKNOWN", false, false, localDeviceId, "Malformed or oversized packet"))
                _state.value = _state.value.copy(error = "Malformed or oversized packet rejected")
                return
            }
            val decoded = MeshProtocol.decodePacket(line)
            if (decoded.isFailure) {
                sendAck(connected, MeshAck("UNKNOWN", false, false, localDeviceId, "Malformed mesh packet"))
                _state.value = _state.value.copy(error = "Malformed mesh packet rejected")
                return
            }
            val meshMessage = decoded.getOrThrow()
            val packet = meshMessage.packet

            // ORDER OF CHECKS:
            // 1. Validate packet
            if (PacketValidator.validate(packet).isFailure) {
                sendAck(connected, MeshAck(packet.messageId, false, false, localDeviceId, "Invalid packet payload"))
                return
            }

            // 2. Check whether packet is already DELIVERED, CLOSED, or EXPIRED
            if (packet.status == com.resq.data.model.PacketStatus.DELIVERED ||
                packet.status == com.resq.data.model.PacketStatus.CLOSED ||
                packet.status == com.resq.data.model.PacketStatus.EXPIRED) {
                sendAck(connected, MeshAck(packet.messageId, false, packet.status == com.resq.data.model.PacketStatus.DELIVERED, localDeviceId, "Packet is already ${packet.status.name}"))
                return
            }

            val finalDelivery = isRescueMode()

            if (packet.priority == com.resq.data.model.EmergencyPriority.CRITICAL || packet.type == com.resq.data.model.EmergencyType.SOS) {
                com.resq.util.SosNotificationManager.showSosReceivedNotification(
                    appContext,
                    meshMessage.forwarderId,
                    packet.text,
                    packet.latitude,
                    packet.longitude
                )
            }

            // 3. Destination check: Check whether this device is the intended Rescue Node
            if (finalDelivery) {
                emergencyRepository.receivePacket(packet, finalDelivery = true).fold(
                    onSuccess = { stored ->
                        emergencyRepository.markClosed(stored.messageId)
                        sendAck(connected, MeshAck(stored.messageId, true, true, localDeviceId))
                        supportDao.insertForwardingLog(
                            ForwardingLog(
                                messageId = stored.messageId,
                                fromDevice = meshMessage.forwarderId,
                                toDevice = localDeviceId,
                                method = "BLUETOOTH",
                                timestamp = System.currentTimeMillis(),
                                result = "RESCUE_RECEIVED"
                            )
                        )
                        _state.value = _state.value.copy(
                            status = "Rescue received ${stored.messageId} (Delivered/Closed)",
                            receivedPacketId = stored.messageId,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        sendAck(connected, MeshAck(packet.messageId, false, true, localDeviceId, error.message))
                        _state.value = _state.value.copy(error = error.message ?: "Incoming packet rejected", status = "Packet not stored")
                    }
                )
                return
            }

            // 4. If this device is NOT the destination, check hop limit
            if (packet.hopCount >= PacketValidator.MAX_HOPS) {
                emergencyRepository.receiveExpiredPacket(packet).fold(
                    onSuccess = { expired ->
                        sendAck(connected, MeshAck(expired.messageId, false, false, localDeviceId, "HOP_LIMIT_EXCEEDED"))
                        supportDao.insertForwardingLog(
                            ForwardingLog(
                                messageId = expired.messageId,
                                fromDevice = meshMessage.forwarderId,
                                toDevice = localDeviceId,
                                method = "BLUETOOTH",
                                timestamp = System.currentTimeMillis(),
                                result = "EXPIRED_HOP_LIMIT"
                            )
                        )
                        _state.value = _state.value.copy(
                            status = "Packet ${expired.messageId} expired (Hop limit ${expired.hopCount}/${PacketValidator.MAX_HOPS} exceeded)",
                            receivedPacketId = expired.messageId,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        sendAck(connected, MeshAck(packet.messageId, false, false, localDeviceId, "HOP_LIMIT_EXCEEDED"))
                    }
                )
                return
            }

            // 5. Otherwise: continue using existing store-and-forward logic
            emergencyRepository.receivePacket(packet, finalDelivery = false).fold(
                onSuccess = { stored ->
                    sendAck(connected, MeshAck(stored.messageId, true, false, localDeviceId))
                    supportDao.insertForwardingLog(
                        ForwardingLog(
                            messageId = stored.messageId,
                            fromDevice = meshMessage.forwarderId,
                            toDevice = localDeviceId,
                            method = "BLUETOOTH",
                            timestamp = System.currentTimeMillis(),
                            result = "RECEIVED"
                        )
                    )
                    _state.value = _state.value.copy(
                        status = "Received ${stored.messageId} from $remote",
                        receivedPacketId = stored.messageId,
                        error = null
                    )
                },
                onFailure = { error ->
                    sendAck(connected, MeshAck(packet.messageId, false, false, localDeviceId, error.message))
                    _state.value = _state.value.copy(error = error.message ?: "Incoming packet rejected", status = "Packet not stored")
                }
            )
        }
    }

    private fun sendAck(socket: BluetoothSocket, ack: MeshAck) {
        runCatching {
            val writer = socket.outputStream.bufferedWriter()
            writer.write(MeshProtocol.encodeAck(ack))
            writer.newLine()
            writer.flush()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addPeer(device: BluetoothDevice) {
        val peer = PeerDevice(device.name ?: "Unknown device", device.address, device.bondState == BluetoothDevice.BOND_BONDED)
        _state.value = _state.value.copy(peers = mergePeers(listOf(peer), _state.value.peers))
    }

    private fun mergePeers(first: List<PeerDevice>, second: List<PeerDevice>): List<PeerDevice> =
        (first + second).associateBy { it.address }.values.sortedWith(compareByDescending<PeerDevice> { it.paired }.thenBy { it.name.lowercase() })

    @Suppress("DEPRECATION")
    private fun deviceFrom(intent: Intent): BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

    @SuppressLint("MissingPermission")
    fun close() {
        runCatching { adapter?.cancelDiscovery() }
        runCatching { serverSocket?.close() }
        serverJob?.cancel()
        scope.cancel()
        if (receiverRegistered) runCatching { appContext.unregisterReceiver(receiver) }
        receiverRegistered = false
    }

    companion object {
        const val SERVICE_NAME = "ResQ Emergency Mesh"
        val SERVICE_UUID: UUID = UUID.fromString("4c0f2f96-907d-4a2b-a537-4dbd9bd5b278")
        private const val MAX_PACKET_CHARS = 8_192
        private const val ACK_TIMEOUT_MS = 15_000L
    }
}
