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

import com.joi.ghosthand.routes.action.buildActionEffectDisclosure
import com.joi.ghosthand.routes.action.observeScrollSurfaceChange
import com.joi.ghosthand.routes.action.toActionEffectObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionRouteHandlersTest {
    @Test
    fun surfaceObservationCarriesAfterSnapshotForEvidenceProjection() {
        val before = snapshot("before", "com.example", "FirstActivity")
        val after = snapshot("after", "com.example", "SecondActivity")

        val observation = observeScrollSurfaceChange(before, after)

        assertTrue(observation.surfaceChanged)
        assertEquals("after", observation.afterSnapshotToken)
        assertEquals(after, observation.afterSnapshot)
        assertEquals(true, observation.toActionEffectObservation().stateChanged)
    }

    @Test
    fun ambiguityDisclosureOnlyAppearsWhenActionDispatchedWithoutObservedChange() {
        assertNotNull(buildActionEffectDisclosure("/tap", true, false))
        assertNull(buildActionEffectDisclosure("/tap", true, true))
        assertNull(buildActionEffectDisclosure("/tap", false, false))
    }

    private fun snapshot(token: String, packageName: String, activity: String): AccessibilityTreeSnapshot {
        return AccessibilityTreeSnapshot(
            packageName = packageName,
            activity = activity,
            snapshotToken = token,
            capturedAt = "2026-04-04T00:00:00Z",
            nodes = listOf(
                FlatAccessibilityNode(
                    nodeId = "n0",
                    text = "Node",
                    contentDesc = null,
                    resourceId = null,
                    className = "android.widget.TextView",
                    clickable = true,
                    editable = false,
                    enabled = true,
                    focused = false,
                    scrollable = false,
                    centerX = 10,
                    centerY = 10,
                    bounds = NodeBounds(0, 0, 20, 20)
                )
            ),
            foregroundStableDuringCapture = true
        )
    }
}
