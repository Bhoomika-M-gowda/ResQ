package com.resq.ai.decision

import com.resq.data.model.EmergencyPriority

enum class CommunicationMethod { BLUETOOTH, WIFI_LOCAL, STORE_RETRY }

data class DecisionInput(
    val priority: EmergencyPriority,
    val packetSizeBytes: Int,
    val batteryPercent: Int,
    val bluetoothPeerAvailable: Boolean,
    val wifiPeerAvailable: Boolean,
    val bluetoothRecentFailures: Int,
    val wifiRecentFailures: Int
)

data class DecisionResult(
    val method: CommunicationMethod,
    val reason: String,
    val factors: List<String>
)

object CommunicationDecisionEngine {
    private const val SMALL_PACKET_BYTES = 8_192
    private const val LOW_BATTERY_PERCENT = 20
    private const val FAILURE_THRESHOLD = 2

    fun decide(input: DecisionInput): DecisionResult {
        val factors = listOf(
            "Priority: ${input.priority}",
            "Packet: ${input.packetSizeBytes} bytes",
            "Battery: ${input.batteryPercent}%",
            "Bluetooth peer: ${yesNo(input.bluetoothPeerAvailable)}",
            "Wi-Fi peer: ${yesNo(input.wifiPeerAvailable)}",
            "Recent failures: BT ${input.bluetoothRecentFailures}, Wi-Fi ${input.wifiRecentFailures}"
        )
        val choice = when {
            !input.bluetoothPeerAvailable && !input.wifiPeerAvailable ->
                CommunicationMethod.STORE_RETRY to "No suitable peer is available; keep the packet in Room and retry later."
            input.bluetoothRecentFailures >= FAILURE_THRESHOLD && input.wifiPeerAvailable ->
                CommunicationMethod.WIFI_LOCAL to "Bluetooth failed repeatedly, so use the available Wi-Fi Direct path."
            input.wifiRecentFailures >= FAILURE_THRESHOLD && input.bluetoothPeerAvailable ->
                CommunicationMethod.BLUETOOTH to "Wi-Fi failed repeatedly, so fall back to the reliable Bluetooth path."
            input.batteryPercent <= LOW_BATTERY_PERCENT && input.bluetoothPeerAvailable ->
                CommunicationMethod.BLUETOOTH to "Battery is low and the packet is compact; Bluetooth is the lower-cost path."
            input.packetSizeBytes > SMALL_PACKET_BYTES && input.wifiPeerAvailable ->
                CommunicationMethod.WIFI_LOCAL to "The transfer is larger and a Wi-Fi Direct peer is available."
            input.priority == EmergencyPriority.CRITICAL && input.bluetoothPeerAvailable ->
                CommunicationMethod.BLUETOOTH to "Critical compact packet: use the immediately available paired Bluetooth peer."
            input.bluetoothPeerAvailable ->
                CommunicationMethod.BLUETOOTH to "A paired Bluetooth peer is available for this compact emergency packet."
            else -> CommunicationMethod.WIFI_LOCAL to "Wi-Fi Direct is the available local peer path."
        }
        return DecisionResult(choice.first, choice.second, factors)
    }

    private fun yesNo(value: Boolean) = if (value) "yes" else "no"
}
