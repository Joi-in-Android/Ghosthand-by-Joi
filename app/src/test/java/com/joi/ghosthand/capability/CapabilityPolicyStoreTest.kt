/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.capability

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

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CapabilityPolicyStoreTest {
    @Test
    fun preservesAccessibilityAndScreenshotPolicyBooleans() {
        val tempFile = Files.createTempFile("ghosthand-policy", ".preferences_pb").toFile()
        val store = CapabilityPolicyStore(
            dataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { tempFile }
            )
        )

        store.setAllowed(GhosthandCapability.Accessibility, true)
        store.setAllowed(GhosthandCapability.Screenshot, false)

        val snapshot = runBlocking {
            repeat(20) {
                val value = store.snapshot()
                if (value.accessibilityAllowed) {
                    return@runBlocking value
                }
                delay(20)
            }
            store.snapshot()
        }
        assertTrue(snapshot.accessibilityAllowed)
        assertFalse(snapshot.screenshotAllowed)
    }

    @Test
    fun migratedSnapshotUsesOnlySupportedLegacyKeys() {
        val migrated = CapabilityPolicyStore.migratedSnapshot(
            legacyPreferences = mapOf(
                "capability.accessibility" to true,
                "capability.screenshot" to true,
                "capability.root" to false
            )
        )

        assertEquals(
            CapabilityPolicySnapshot(
                accessibilityAllowed = true,
                screenshotAllowed = true
            ),
            migrated
        )
    }

    @Test
    fun setAllowedUpdatesSnapshotImmediatelyBeforeDatastoreRoundTrip() {
        val tempFile = Files.createTempFile("ghosthand-policy-immediate", ".preferences_pb").toFile()
        val store = CapabilityPolicyStore(
            dataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { tempFile }
            )
        )

        store.setAllowed(GhosthandCapability.Screenshot, true)

        assertTrue(store.snapshot().screenshotAllowed)
        val persisted = runBlocking { store.observe().first { it.screenshotAllowed } }
        assertTrue(persisted.screenshotAllowed)
    }
}
