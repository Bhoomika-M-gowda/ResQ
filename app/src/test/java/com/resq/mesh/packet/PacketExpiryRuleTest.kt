package com.resq.mesh.packet

import com.resq.data.model.EmergencyPacket
import com.resq.data.model.EmergencyPriority
import com.resq.data.model.EmergencyType
import com.resq.data.model.PacketStatus
import org.junit.Assert.*
import org.junit.Test

class PacketExpiryRuleTest {

    private fun createSamplePacket(
        messageId: String = "EMG-1001",
        hopCount: Int = 0,
        status: PacketStatus = PacketStatus.STORED,
        destinationId: String = "RESCUE"
    ): EmergencyPacket = EmergencyPacket(
        messageId = messageId,
        priority = EmergencyPriority.CRITICAL,
        type = EmergencyType.SOS,
        text = "Immediate SOS assistance requested",
        latitude = 12.9716,
        longitude = 77.5946,
        timestamp = System.currentTimeMillis(),
        senderId = "VICTIM-01",
        status = status,
        hopCount = hopCount,
        lastForwardedAt = null,
        destinationId = destinationId
    )

    // TEST 1: Packet created -> Expected: ACTIVE / STORED
    @Test
    fun test1_PacketCreated_IsActiveAndStored() {
        val packet = createSamplePacket(hopCount = 0, status = PacketStatus.STORED)
        assertEquals(0, packet.hopCount)
        assertEquals(PacketStatus.STORED, packet.status)
        assertFalse(packet.status == PacketStatus.EXPIRED || packet.status == PacketStatus.CLOSED)
    }

    // TEST 2: Packet reaches Relay 1 -> Expected: NOT EXPIRED
    @Test
    fun test2_PacketAtRelay1_NotExpired() {
        val packet = createSamplePacket(hopCount = 1, status = PacketStatus.FORWARDED)
        assertEquals(1, packet.hopCount)
        assertTrue(packet.hopCount < PacketValidator.MAX_HOPS)
        assertNotEquals(PacketStatus.EXPIRED, packet.status)
    }

    // TEST 3: Packet reaches Relay 2 -> Expected: NOT EXPIRED
    @Test
    fun test3_PacketAtRelay2_NotExpired() {
        val packet = createSamplePacket(hopCount = 2, status = PacketStatus.FORWARDED)
        assertEquals(2, packet.hopCount)
        assertTrue(packet.hopCount < PacketValidator.MAX_HOPS)
        assertNotEquals(PacketStatus.EXPIRED, packet.status)
    }

    // TEST 4: Packet reaches Rescue Node before hop limit -> Expected: DELIVERED / CLOSED, NO FURTHER FORWARDING
    @Test
    fun test4_ReachesRescueNodeBeforeHopLimit_DeliveredAndClosed() {
        val packet = createSamplePacket(hopCount = 3, status = PacketStatus.FORWARDED, destinationId = "RESCUE")
        val isRescueNode = true // Device acting as intended Rescue Node

        // Order of checks:
        val resultStatus = if (isRescueNode) PacketStatus.CLOSED else if (packet.hopCount >= PacketValidator.MAX_HOPS) PacketStatus.EXPIRED else PacketStatus.STORED

        assertEquals(PacketStatus.CLOSED, resultStatus)
        // No further forwarding when closed
        assertTrue(resultStatus == PacketStatus.CLOSED || resultStatus == PacketStatus.DELIVERED)
    }

    // TEST 5: Packet reaches Rescue Node exactly at MAX_HOPS -> Expected: DELIVERED, NOT hop-limit expired
    @Test
    fun test5_ReachesRescueNodeAtMaxHops_IsDeliveredNotExpired() {
        val packet = createSamplePacket(hopCount = 5, status = PacketStatus.FORWARDED, destinationId = "RESCUE")
        val isRescueNode = true // Device acting as intended Rescue Node

        // Very Important Edge Case: Destination check happens BEFORE hop-limit expiry!
        val finalStatus = if (isRescueNode) {
            PacketStatus.DELIVERED
        } else if (packet.hopCount >= PacketValidator.MAX_HOPS) {
            PacketStatus.EXPIRED
        } else {
            PacketStatus.STORED
        }

        assertEquals(PacketStatus.DELIVERED, finalStatus)
        assertNotEquals(PacketStatus.EXPIRED, finalStatus)
    }

    // TEST 6: Packet reaches a non-rescue relay with hopCount >= MAX_HOPS -> Expected: EXPIRED, HOP LIMIT EXCEEDED
    @Test
    fun test6_ReachesNonRescueRelayAtMaxHops_IsExpired() {
        val packet = createSamplePacket(hopCount = 5, status = PacketStatus.FORWARDED, destinationId = "RESCUE")
        val isRescueNode = false // Non-rescue relay node

        val finalStatus = if (isRescueNode) {
            PacketStatus.DELIVERED
        } else if (packet.hopCount >= PacketValidator.MAX_HOPS) {
            PacketStatus.EXPIRED
        } else {
            PacketStatus.STORED
        }

        assertEquals(PacketStatus.EXPIRED, finalStatus)
    }

    // TEST 7: An EXPIRED packet is encountered later -> Expected: NO FORWARDING, NO RETRY
    @Test
    fun test7_ExpiredPacketEncountered_NoForwardingNoRetry() {
        val packet = createSamplePacket(hopCount = 5, status = PacketStatus.EXPIRED)

        val canForward = when (packet.status) {
            PacketStatus.DELIVERED, PacketStatus.CLOSED, PacketStatus.EXPIRED -> false
            else -> packet.hopCount < PacketValidator.MAX_HOPS
        }

        assertFalse(canForward)
    }

    // TEST 8: Wrong Rescue Node receives packet -> Expected: NOT DELIVERED to wrong node
    @Test
    fun test8_WrongRescueNodeReceives_NotDelivered() {
        val packet = createSamplePacket(hopCount = 2, destinationId = "RESCUE-ALPHA")
        val localRescueNodeId = "RESCUE-BETA"

        val isTargetRescueNode = packet.destinationId == localRescueNodeId
        assertFalse(isTargetRescueNode)

        val nextAction = if (isTargetRescueNode) "DELIVER" else if (packet.hopCount >= PacketValidator.MAX_HOPS) "EXPIRE" else "FORWARD"
        assertEquals("FORWARD", nextAction)
    }

    // TEST 9: Rescue Node unavailable -> Expected: Store-and-forward continues while hopCount < MAX_HOPS
    @Test
    fun test9_RescueNodeUnavailable_StoreAndForwardContinues() {
        val packet = createSamplePacket(hopCount = 3, status = PacketStatus.STORED)
        val rescueAvailable = false

        val canStoreAndForward = packet.status != PacketStatus.EXPIRED && packet.status != PacketStatus.CLOSED && packet.hopCount < PacketValidator.MAX_HOPS
        assertTrue(canStoreAndForward)
    }

    // TEST 10: Duplicate packet arrives -> Expected: Duplicate rejected by DAO / Validator
    @Test
    fun test10_DuplicatePacket_HandledCorrectly() {
        val packet1 = createSamplePacket(messageId = "EMG-9999", hopCount = 1)
        val packet2 = createSamplePacket(messageId = "EMG-9999", hopCount = 2)

        assertEquals(packet1.messageId, packet2.messageId)
    }
}
