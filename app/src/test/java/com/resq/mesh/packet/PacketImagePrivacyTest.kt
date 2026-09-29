package com.resq.mesh.packet

import com.resq.data.model.EmergencyPacket
import com.resq.data.model.EmergencyPriority
import com.resq.data.model.EmergencyType
import com.resq.data.model.PacketStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketImagePrivacyTest {
    @Test
    fun localImagePathIsNotTransmittedOverMesh() {
        val packet = EmergencyPacket(
            messageId = "EMG-PHOTO",
            priority = EmergencyPriority.URGENT,
            type = EmergencyType.FIRE,
            text = "Fire is visible in the area",
            latitude = 12.9716,
            longitude = 77.5946,
            timestamp = 1L,
            senderId = "DEVICE-A",
            status = PacketStatus.STORED,
            hopCount = 0,
            lastForwardedAt = null,
            imageLocalPath = "/private/emergency-photo.jpg"
        )

        val encoded = PacketJsonCodec.encode(packet)
        assertFalse(encoded.contains("imageLocalPath"))
        val decoded = PacketJsonCodec.decode(encoded)
        assertTrue(decoded.isSuccess)
        assertNull(decoded.getOrThrow().imageLocalPath)
    }
}
