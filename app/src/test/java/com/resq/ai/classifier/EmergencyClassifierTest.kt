package com.resq.ai.classifier

import com.resq.data.model.EmergencyPriority
import com.resq.data.model.EmergencyType
import org.junit.Assert.assertEquals
import org.junit.Test

class EmergencyClassifierTest {
    @Test fun trappedFloodIsCriticalFlood() {
        val result = EmergencyClassifier.classify("There are people trapped near the flooded road.")
        assertEquals(EmergencyType.FLOOD, result.type)
        assertEquals(EmergencyPriority.CRITICAL, result.priority)
    }

    @Test fun visibleFireIsUrgent() {
        val result = EmergencyClassifier.classify("Smoke and fire near a house")
        assertEquals(EmergencyType.FIRE, result.type)
        assertEquals(EmergencyPriority.URGENT, result.priority)
    }

    @Test fun manualTypeIsFallback() {
        val result = EmergencyClassifier.classify("The route needs inspection", EmergencyType.BLOCKED_ROAD)
        assertEquals(EmergencyType.BLOCKED_ROAD, result.type)
        assertEquals(EmergencyPriority.NORMAL, result.priority)
    }
}
