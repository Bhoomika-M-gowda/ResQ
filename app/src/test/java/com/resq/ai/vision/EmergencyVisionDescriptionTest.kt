package com.resq.ai.vision

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyVisionDescriptionTest {
    @Test
    fun roadAndWaterProducesConservativeDescription() {
        val result = EmergencyVisionDescription.fromWords(listOf("Road", "Water"))
        assertTrue(result.contains("Water is visible"))
        assertFalse(result.contains("user is stuck", ignoreCase = true))
    }

    @Test
    fun genericLabelsDoNotInventVictimsOrInjuries() {
        val result = EmergencyVisionDescription.fromWords(listOf("Building", "Sky", "Tree"))
        assertFalse(result.contains("injured", ignoreCase = true))
        assertFalse(result.contains("victim", ignoreCase = true))
        assertFalse(result.contains("trapped", ignoreCase = true))
    }
}
