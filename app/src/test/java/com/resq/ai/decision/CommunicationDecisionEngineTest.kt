package com.resq.ai.decision

import com.resq.data.model.EmergencyPriority
import org.junit.Assert.assertEquals
import org.junit.Test

class CommunicationDecisionEngineTest {
    @Test fun noPeerStoresAndRetries() {
        val result = CommunicationDecisionEngine.decide(input(bluetooth = false, wifi = false))
        assertEquals(CommunicationMethod.STORE_RETRY, result.method)
    }

    @Test fun criticalCompactPacketUsesBluetooth() {
        val result = CommunicationDecisionEngine.decide(input(bluetooth = true, wifi = true))
        assertEquals(CommunicationMethod.BLUETOOTH, result.method)
    }

    @Test fun repeatedBluetoothFailuresUseWifi() {
        val result = CommunicationDecisionEngine.decide(input(bluetooth = true, wifi = true, bluetoothFailures = 2))
        assertEquals(CommunicationMethod.WIFI_LOCAL, result.method)
    }

    private fun input(bluetooth: Boolean, wifi: Boolean, bluetoothFailures: Int = 0) = DecisionInput(
        priority = EmergencyPriority.CRITICAL,
        packetSizeBytes = 500,
        batteryPercent = 75,
        bluetoothPeerAvailable = bluetooth,
        wifiPeerAvailable = wifi,
        bluetoothRecentFailures = bluetoothFailures,
        wifiRecentFailures = 0
    )
}
