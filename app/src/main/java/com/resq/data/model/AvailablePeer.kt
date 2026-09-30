package com.resq.data.model

import com.resq.mesh.bluetooth.PeerDevice
import com.resq.mesh.wifi.WifiPeer

enum class PeerConnectionType {
    BLUETOOTH,
    WIFI_DIRECT
}

data class AvailablePeer(
    val id: String,
    val name: String,
    val address: String,
    val connectionType: PeerConnectionType,
    val isAvailable: Boolean,
    val statusText: String,
    val rawBluetoothPeer: PeerDevice? = null,
    val rawWifiPeer: WifiPeer? = null
)

enum class PeerSendStatus {
    PENDING,
    SENDING,
    SUCCESS,
    FAILED,
    SKIPPED_DUPLICATE
}

data class PeerSendProgress(
    val peerId: String,
    val peerName: String,
    val connectionType: PeerConnectionType,
    val index: Int,
    val total: Int,
    val status: PeerSendStatus,
    val message: String = ""
)

data class BatchSendProgressState(
    val isSending: Boolean = false,
    val totalSelected: Int = 0,
    val currentSendingIndex: Int = 0,
    val peerProgressList: List<PeerSendProgress> = emptyList(),
    val finalSummaryMessage: String? = null
)
