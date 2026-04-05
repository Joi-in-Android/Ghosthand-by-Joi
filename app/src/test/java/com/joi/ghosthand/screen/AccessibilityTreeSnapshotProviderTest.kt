/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.screen

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

import com.joi.ghosthand.screen.read.assessSnapshotFreshness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityTreeSnapshotProviderTest {
    @Test
    fun acceptsStableAlignedForegroundCapture() {
        val assessment = assessSnapshotFreshness(
            foregroundBefore = ForegroundAppSnapshot(
                packageName = "com.android.settings",
                activity = "SettingsActivity"
            ),
            foregroundAfter = ForegroundAppSnapshot(
                packageName = "com.android.settings",
                activity = "SettingsActivity"
            ),
            rootPackageName = "com.android.settings",
            finalAttempt = false
        )

        assertTrue(assessment.acceptSnapshot)
        assertTrue(assessment.foregroundStableDuringCapture)
        assertEquals("SettingsActivity", assessment.activity)
        assertTrue(assessment.warnings.isEmpty())
    }

    @Test
    fun retriesWhenForegroundChangesDuringCapture() {
        val assessment = assessSnapshotFreshness(
            foregroundBefore = ForegroundAppSnapshot(
                packageName = "com.android.settings",
                activity = "SettingsActivity"
            ),
            foregroundAfter = ForegroundAppSnapshot(
                packageName = "com.miui.home",
                activity = "Launcher"
            ),
            rootPackageName = "com.android.settings",
            finalAttempt = false
        )

        assertFalse(assessment.acceptSnapshot)
        assertFalse(assessment.foregroundStableDuringCapture)
        assertEquals(null, assessment.activity)
        assertTrue(assessment.warnings.contains("foreground_changed_during_capture"))
        assertTrue(assessment.warnings.contains("surface_package_mismatch"))
    }

    @Test
    fun finalAttemptReturnsSnapshotWithFreshnessWarning() {
        val assessment = assessSnapshotFreshness(
            foregroundBefore = ForegroundAppSnapshot(
                packageName = "com.android.settings",
                activity = "SettingsActivity"
            ),
            foregroundAfter = ForegroundAppSnapshot(
                packageName = "com.android.settings",
                activity = "NextActivity"
            ),
            rootPackageName = "com.android.settings",
            finalAttempt = true
        )

        assertTrue(assessment.acceptSnapshot)
        assertFalse(assessment.foregroundStableDuringCapture)
        assertEquals(null, assessment.activity)
        assertEquals(listOf("foreground_changed_during_capture"), assessment.warnings)
    }
}
