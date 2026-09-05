package com.example.fingerprint.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EventTimingHelperTest {

    @Test
    fun testGracePeriodDefaults() {
        val scheduled = 1000000L

        // IN: default grace is 0 min, threshold is scheduled + 60s
        assertNull(EventTimingHelper.getLateDurationMillis(EventType.IN, scheduled, scheduled + 30_000L))
        assertNotNull(EventTimingHelper.getLateDurationMillis(EventType.IN, scheduled, scheduled + 61_000L))

        // OUT_IN: default grace is 15 min, threshold is scheduled + 15m + 60s
        assertNull(EventTimingHelper.getLateDurationMillis(EventType.OUT_IN, scheduled, scheduled + 14 * 60 * 1000L))
        assertNotNull(EventTimingHelper.getLateDurationMillis(EventType.OUT_IN, scheduled, scheduled + 16 * 60 * 1000L + 1000L))

        // OUT: default grace is 30 min, threshold is scheduled + 30m + 60s
        assertNull(EventTimingHelper.getLateDurationMillis(EventType.OUT, scheduled, scheduled + 29 * 60 * 1000L))
        assertNotNull(EventTimingHelper.getLateDurationMillis(EventType.OUT, scheduled, scheduled + 31 * 60 * 1000L + 1000L))
    }

    @Test
    fun testCustomGracePeriods() {
        val scheduled = 1000000L

        // Custom IN grace: 10 mins
        assertNull(EventTimingHelper.getLateDurationMillis(EventType.IN, scheduled, scheduled + 9 * 60 * 1000L, graceMinutesIn = 10))
        val lateIn = EventTimingHelper.getLateDurationMillis(EventType.IN, scheduled, scheduled + 15 * 60 * 1000L, graceMinutesIn = 10)
        assertNotNull(lateIn)
        assertEquals(5 * 60 * 1000L, lateIn) // 15m - 10m grace = 5m delay

        // Custom OUT_IN grace: 5 mins
        val lateOutIn = EventTimingHelper.getLateDurationMillis(EventType.OUT_IN, scheduled, scheduled + 10 * 60 * 1000L, graceMinutesOutIn = 5)
        assertNotNull(lateOutIn)
        assertEquals(5 * 60 * 1000L, lateOutIn)

        // Custom OUT grace: 45 mins
        assertNull(EventTimingHelper.getLateDurationMillis(EventType.OUT, scheduled, scheduled + 40 * 60 * 1000L, graceMinutesOut = 45))
        val lateOut = EventTimingHelper.getLateDurationMillis(EventType.OUT, scheduled, scheduled + 50 * 60 * 1000L, graceMinutesOut = 45)
        assertNotNull(lateOut)
        assertEquals(5 * 60 * 1000L, lateOut)
    }

    @Test
    fun testFormatLateLabelLocalization() {
        val late5m = 5 * 60 * 1000L
        val late1h30m = (90 * 60 * 1000L)

        // Arabic
        assertEquals("تأخير 5 دقيقة", EventTimingHelper.formatLateLabel(late5m, isArabic = true))
        assertEquals("تأخير 1 ساعة و 30 دقيقة", EventTimingHelper.formatLateLabel(late1h30m, isArabic = true))

        // English
        assertEquals("Late 5 minutes", EventTimingHelper.formatLateLabel(late5m, isArabic = false))
        assertEquals("Late 1 hr 30 mins", EventTimingHelper.formatLateLabel(late1h30m, isArabic = false))
    }
}
