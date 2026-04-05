/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.routes

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

import com.joi.ghosthand.observation.GhosthandObservationLog
import com.joi.ghosthand.routes.observation.ObservationRouteHandlers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EventsRouteHandlersTest {
    @Test
    fun eventsRouteReturnsCursorFilteredBatch() {
        val timestamps = listOf(
            Instant.parse("2026-04-04T00:00:00Z"),
            Instant.parse("2026-04-04T00:00:01Z")
        ).iterator()
        val log = GhosthandObservationLog(
            retentionLimit = 10,
            nowProvider = { timestamps.next() }
        )
        log.append(type = "foreground_changed", packageName = "pkg.one")
        log.append(type = "window_changed", packageName = "pkg.one", activity = "MainActivity")

        val result = ObservationRouteHandlers(log).queryEvents(
            mapOf("since" to "1", "limit" to "10")
        )
        val batch = result.batch!!

        assertEquals(1, batch.events.size)
        assertEquals("window_changed", batch.events[0].type)
        assertEquals(2L, batch.latestCursor)
        assertEquals(2L, batch.nextCursor)
        assertFalse(batch.droppedBeforeCursor)
    }

    @Test
    fun eventsRouteRejectsMalformedCursorParameters() {
        val result = ObservationRouteHandlers(GhosthandObservationLog()).queryEvents(
            mapOf("since" to "oops")
        )

        assertNull(result.batch)
        assertTrue(result.errorMessage!!.contains("since"))
    }
}
