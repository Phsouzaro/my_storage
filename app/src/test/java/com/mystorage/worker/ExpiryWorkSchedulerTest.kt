package com.mystorage.worker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class ExpiryWorkSchedulerTest {

    @Test
    fun `antes do horario agenda para hoje`() {
        val now = LocalDateTime.of(2026, 11, 6, 7, 30)
        assertEquals(Duration.ofMinutes(90), ExpiryWorkScheduler.delayUntil(9, now))
    }

    @Test
    fun `depois do horario agenda para amanha`() {
        val now = LocalDateTime.of(2026, 11, 6, 14, 0)
        assertEquals(Duration.ofHours(19), ExpiryWorkScheduler.delayUntil(9, now))
    }

    @Test
    fun `exatamente no horario agenda para amanha`() {
        val now = LocalDateTime.of(2026, 11, 6, 9, 0)
        assertEquals(Duration.ofHours(24), ExpiryWorkScheduler.delayUntil(9, now))
    }
}
