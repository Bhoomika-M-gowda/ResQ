package com.resq.mesh.packet

import com.resq.data.model.*
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketValidatorTest {
    private val valid = EmergencyPacket(
        messageId = "EMG-TEST",
        priority = EmergencyPriority.CRITICAL,
        type = EmergencyType.SOS,
        text = "Help required",
        latitude = 12.9716,
        longitude = 77.5946,
        timestamp = 1L,
        senderId = "DEVICE-A",
        status = PacketStatus.STORED,
        hopCount = 0,
        lastForwardedAt = null
    )

    @Test fun validPacketPasses() {
        assertTrue(PacketValidator.validate(valid).isSuccess)
    }

    @Test fun blankTextFails() {
        assertTrue(PacketValidator.validate(valid.copy(text = "")).isFailure)
    }

    @Test fun invalidCoordinatesFail() {
        assertTrue(PacketValidator.validate(valid.copy(latitude = 91.0)).isFailure)
    }
}
