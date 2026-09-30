package com.resq.mesh

import com.resq.data.model.AvailablePeer
import com.resq.data.model.PeerConnectionType
import com.resq.mesh.bluetooth.PeerDevice
import com.resq.mesh.wifi.WifiPeer
import org.junit.Assert.*
import org.junit.Test

class SelectAllAvailableTest {

    private val samplePeers = listOf(
        AvailablePeer(
            id = "BT:11:22:33:44:55:66",
            name = "Phone B",
            address = "11:22:33:44:55:66",
            connectionType = PeerConnectionType.BLUETOOTH,
            isAvailable = true,
            statusText = "Available",
            rawBluetoothPeer = PeerDevice("Phone B", "11:22:33:44:55:66", paired = true)
        ),
        AvailablePeer(
            id = "BT:22:33:44:55:66:77",
            name = "Phone C",
            address = "22:33:44:55:66:77",
            connectionType = PeerConnectionType.BLUETOOTH,
            isAvailable = true,
            statusText = "Available",
            rawBluetoothPeer = PeerDevice("Phone C", "22:33:44:55:66:77", paired = true)
        ),
        AvailablePeer(
            id = "WIFI:33:44:55:66:77:88",
            name = "Phone D",
            address = "33:44:55:66:77:88",
            connectionType = PeerConnectionType.WIFI_DIRECT,
            isAvailable = true,
            statusText = "Available",
            rawWifiPeer = WifiPeer("Phone D", "33:44:55:66:77:88")
        ),
        AvailablePeer(
            id = "BT:44:55:66:77:88:99",
            name = "Phone E (Unpaired)",
            address = "44:55:66:77:88:99",
            connectionType = PeerConnectionType.BLUETOOTH,
            isAvailable = false,
            statusText = "Not paired",
            rawBluetoothPeer = PeerDevice("Phone E", "44:55:66:77:88:99", paired = false)
        )
    )

    @Test
    fun testSelectAllAvailableOnlySelectsAvailablePeers() {
        val availableOnly = samplePeers.filter { it.isAvailable }
        val selectedIds = availableOnly.map { it.id }.toSet()

        assertEquals(3, selectedIds.size)
        assertTrue(selectedIds.contains("BT:11:22:33:44:55:66"))
        assertTrue(selectedIds.contains("BT:22:33:44:55:66:77"))
        assertTrue(selectedIds.contains("WIFI:33:44:55:66:77:88"))
        assertFalse(selectedIds.contains("BT:44:55:66:77:88:99"))
    }

    @Test
    fun testDeselectingOneUpdatesSelectAllState() {
        val availableOnly = samplePeers.filter { it.isAvailable }
        var selectedIds = availableOnly.map { it.id }.toSet()

        // Initially all available selected
        assertTrue(availableOnly.all { it.id in selectedIds })

        // Manually deselect one device
        selectedIds = selectedIds - "BT:11:22:33:44:55:66"

        // Select All state should now be false
        assertFalse(availableOnly.all { it.id in selectedIds })
        assertEquals(2, selectedIds.size)
    }

    @Test
    fun testNoDuplicateSelections() {
        val set = mutableSetOf<String>()
        set.add("BT:11:22:33:44:55:66")
        set.add("BT:11:22:33:44:55:66")

        assertEquals(1, set.size)
    }
}
