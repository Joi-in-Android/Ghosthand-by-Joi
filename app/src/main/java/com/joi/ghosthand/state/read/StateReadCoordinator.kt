/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.state.read

import com.joi.ghosthand.screen.read.AccessibilityTreeSnapshotResult
import com.joi.ghosthand.capability.CapabilityAccessSnapshot
import com.joi.ghosthand.state.device.DeviceSnapshotProvider
import com.joi.ghosthand.state.device.ForegroundAppProvider
import com.joi.ghosthand.state.diagnostics.HomeDiagnosticsProvider
import com.joi.ghosthand.state.device.PermissionSnapshotProvider
import com.joi.ghosthand.state.runtime.RuntimeState

import com.joi.ghosthand.R

import android.os.SystemClock
import com.joi.ghosthand.capability.CapabilityAccessResolver
import com.joi.ghosthand.state.StatePayloadComposer
import com.joi.ghosthand.state.health.StateHealthPayloads
import org.json.JSONObject

internal class StateReadCoordinator(
    private val runtimeStateProvider: () -> RuntimeState,
    private val treeSnapshotProvider: () -> AccessibilityTreeSnapshotResult,
    private val homeDiagnosticsProvider: HomeDiagnosticsProvider,
    private val deviceSnapshotProvider: DeviceSnapshotProvider,
    private val foregroundAppProvider: ForegroundAppProvider,
    private val permissionSnapshotProvider: PermissionSnapshotProvider,
    private val accessibilityStatusProvider: AccessibilityStatusProvider,
    private val capabilityAccessResolver: CapabilityAccessResolver
) {
    fun capabilityAccessSnapshot(): CapabilityAccessSnapshot {
        return capabilityAccessResolver.capabilityAccessSnapshot()
    }

    fun createStatePayload(): JSONObject {
        val runtimeState = runtimeStateProvider()
        val diagnosticsSnapshot = homeDiagnosticsProvider.snapshot()
        val deviceSnapshot = deviceSnapshotProvider.snapshot()
        val foregroundSnapshot = foregroundAppProvider.snapshot()
        val permissionSnapshot = permissionSnapshotProvider.snapshot()
        val accessibilitySnapshot = capabilityAccessResolver.accessibilityStatusSnapshot()
        val capabilityAccess = capabilityAccessResolver.capabilityAccessSnapshot(accessibilitySnapshot)
        val runtimeUptimeMs = runtimeState.appStartedAtElapsedRealtimeMs?.let {
            (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L)
        }

        return StatePayloadComposer.createStatePayload(
            runtimeState = runtimeState,
            runtimeReady = StateHealthPayloads.runtimeReady(runtimeState),
            runtimeUptimeMs = runtimeUptimeMs,
            diagnosticsSnapshot = diagnosticsSnapshot,
            deviceSnapshot = deviceSnapshot,
            foregroundSnapshot = foregroundSnapshot,
            accessibilitySnapshot = accessibilitySnapshot,
            capabilityAccess = capabilityAccess,
            permissionSnapshot = permissionSnapshot
        )
    }

    fun createForegroundPayload(): JSONObject {
        return StateHealthPayloads.createForegroundPayload(
            foregroundSnapshot = foregroundAppProvider.snapshot(),
            toJson = foregroundAppProvider::toJson
        )
    }

    fun createDevicePayload(): JSONObject {
        return StateHealthPayloads.createDevicePayload(
            deviceSnapshot = deviceSnapshotProvider.snapshot(),
            foregroundSnapshot = foregroundAppProvider.snapshot()
        )
    }

    fun createInfoPayload(): JSONObject {
        return StateHealthPayloads.createInfoPayload(
            treeResult = treeSnapshotProvider(),
            deviceSnapshot = deviceSnapshotProvider.snapshot(),
            foregroundSnapshot = foregroundAppProvider.snapshot()
        )
    }
}
