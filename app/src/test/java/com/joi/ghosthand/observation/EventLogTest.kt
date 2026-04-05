/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.observation

import com.joi.ghosthand.capability.*
import com.joi.ghosthand.catalog.*
import com.joi.ghosthand.integration.github.*
import com.joi.ghosthand.integration.projection.*
import com.joi.ghosthand.interaction.accessibility.*
import com.joi.ghosthand.interaction.clipboard.*
import com.joi.ghosthand.interaction.effects.*
import com.joi.ghosthand.interaction.execution.*
import com.joi.ghosthand.notification.*
import com.joi.ghosthand.payload.*
import com.joi.ghosthand.preview.*
import com.joi.ghosthand.screen.find.*
import com.joi.ghosthand.screen.ocr.*
import com.joi.ghosthand.screen.read.*
import com.joi.ghosthand.screen.summary.*
import com.joi.ghosthand.server.*
import com.joi.ghosthand.server.http.*
import com.joi.ghosthand.service.accessibility.*
import com.joi.ghosthand.service.notification.*
import com.joi.ghosthand.service.runtime.*
import com.joi.ghosthand.state.*
import com.joi.ghosthand.state.device.*
import com.joi.ghosthand.state.diagnostics.*
import com.joi.ghosthand.state.health.*
import com.joi.ghosthand.state.read.*
import com.joi.ghosthand.state.runtime.*
import com.joi.ghosthand.state.summary.*
import com.joi.ghosthand.ui.common.dialog.*
import com.joi.ghosthand.ui.common.model.*
import com.joi.ghosthand.ui.diagnostics.*
import com.joi.ghosthand.ui.main.*
import com.joi.ghosthand.ui.permissions.*
import com.joi.ghosthand.wait.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EventLogTest {
    @Test
    fun readSinceReturnsOrderedEventsAndTracksRetentionBoundaries() {
        val timestamps = listOf(
            Instant.parse("2026-04-04T00:00:00Z"),
            Instant.parse("2026-04-04T00:00:01Z"),
            Instant.parse("2026-04-04T00:00:02Z")
        ).iterator()
        val log = GhosthandObservationLog(
            retentionLimit = 2,
            nowProvider = { timestamps.next() }
        )

        log.append(type = "foreground_changed", packageName = "pkg.one")
        log.append(type = "window_changed", packageName = "pkg.two", activity = "ExampleActivity")
        log.append(type = "screen_readability_changed", packageName = "pkg.two")

        val batch = log.readSince(sinceCursor = 0)

        assertEquals(2, batch.events.size)
        assertEquals(2L, batch.oldestCursor)
        assertEquals(3L, batch.latestCursor)
        assertEquals(3L, batch.nextCursor)
        assertTrue(batch.droppedBeforeCursor)
        assertEquals(listOf(2L, 3L), batch.events.map { it.cursor })
        assertEquals(listOf("window_changed", "screen_readability_changed"), batch.events.map { it.type })
    }

    @Test
    fun readSinceFiltersEventsAfterRequestedCursor() {
        val timestamps = listOf(
            Instant.parse("2026-04-04T00:00:00Z"),
            Instant.parse("2026-04-04T00:00:01Z"),
            Instant.parse("2026-04-04T00:00:02Z")
        ).iterator()
        val log = GhosthandObservationLog(
            retentionLimit = 8,
            nowProvider = { timestamps.next() }
        )

        log.append(type = "foreground_changed", packageName = "pkg.one")
        log.append(type = "window_changed", packageName = "pkg.two")
        log.append(type = "preview_became_available", packageName = "pkg.two")

        val batch = log.readSince(sinceCursor = 1)

        assertFalse(batch.droppedBeforeCursor)
        assertEquals(listOf(2L, 3L), batch.events.map { it.cursor })
        assertEquals(1L, batch.requestedSinceCursor)
    }
}
