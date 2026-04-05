/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.routes.action

import com.joi.ghosthand.screen.read.AccessibilityTreeSnapshot
import com.joi.ghosthand.interaction.execution.GesturePoint
import com.joi.ghosthand.interaction.execution.GestureStroke

import com.joi.ghosthand.R

import com.joi.ghosthand.interaction.effects.ActionEvidencePayloads
import com.joi.ghosthand.observation.GhosthandObservationPublisher
import com.joi.ghosthand.routes.badJsonBodyResponse
import com.joi.ghosthand.routes.buildJsonResponse
import com.joi.ghosthand.routes.errorEnvelope
import com.joi.ghosthand.routes.optIntOrNull
import com.joi.ghosthand.routes.optLongOrNull
import com.joi.ghosthand.routes.parseJsonBodyOrNull
import com.joi.ghosthand.routes.successEnvelope
import com.joi.ghosthand.state.StateCoordinator
import com.joi.ghosthand.state.summary.PostActionStateComposer
import org.json.JSONObject

internal class ActionGestureRouteHandlers(
    private val stateCoordinator: StateCoordinator,
    private val observationPublisher: GhosthandObservationPublisher
) {
    fun buildLongpressResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/longpress")
            ?: return badJsonBodyResponse()
        val x = body.optIntOrNull("x")
        val y = body.optIntOrNull("y")
        if (x == null || y == null) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "x and y are required."))
        }
        val durationMs = body.optLongOrNull("durationMs") ?: 500L
        if (durationMs < 100 || durationMs > 10000) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "durationMs must be between 100 and 10000."))
        }
        val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
        val performed = stateCoordinator.performLongPressGesture(x, y, durationMs)
        val observation = if (performed) {
            observeActionSurfaceChange(beforeSnapshot) { stateCoordinator.getTreeSnapshotResult().snapshot }
        } else {
            observeScrollSurfaceChange(beforeSnapshot, beforeSnapshot)
        }
        return if (performed) {
            val actionEffect = observation.toActionEffectObservation()
            val postActionState = PostActionStateComposer.fromObservedEffect(
                actionEffect = actionEffect,
                fallbackSnapshot = observation.afterSnapshot
            )
            observationPublisher.recordActionCompleted(
                route = "/longpress",
                attemptedPath = "gesture_dispatch",
                actionEffect = actionEffect,
                postActionState = postActionState
            )
            buildJsonResponse(
                200,
                successEnvelope(
                    JSONObject(
                        ActionEvidencePayloads.commonFields(
                            performed = true,
                            attemptedPath = "gesture_dispatch",
                            actionEffect = actionEffect,
                            postActionState = postActionState
                        )
                    ),
                    disclosure = buildActionEffectDisclosure("/longpress", true, actionEffect.stateChanged)
                )
            )
        } else {
            buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Long-press gesture failed."))
        }
    }

    fun buildGestureResponse(requestBody: String): String {
        val body = parseJsonBodyOrNull(requestBody, "/gesture")
            ?: return badJsonBodyResponse()
        val type = body.optString("type").trim().lowercase()
        if (type == "pinch_in" || type == "pinch_out") {
            val x = body.optIntOrNull("x") ?: return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "x is required."))
            val y = body.optIntOrNull("y") ?: return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "y is required."))
            val distance = body.optIntOrNull("distance") ?: 200
            val durationMs = body.optLongOrNull("durationMs") ?: 300L
            val half = (distance / 2).coerceAtLeast(20)
            val strokes = if (type == "pinch_in") {
                listOf(
                    GestureStroke(listOf(GesturePoint(x - half, y), GesturePoint(x, y)), durationMs),
                    GestureStroke(listOf(GesturePoint(x + half, y), GesturePoint(x, y)), durationMs)
                )
            } else {
                listOf(
                    GestureStroke(listOf(GesturePoint(x, y), GesturePoint(x - half, y)), durationMs),
                    GestureStroke(listOf(GesturePoint(x, y), GesturePoint(x + half, y)), durationMs)
                )
            }
            val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
            return gestureResponse(
                performed = stateCoordinator.performGesture(strokes),
                beforeSnapshot = beforeSnapshot
            )
        }

        val strokesJson = body.optJSONArray("strokes")
            ?: return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "strokes is required."))
        val strokes = mutableListOf<GestureStroke>()
        for (i in 0 until strokesJson.length()) {
            val strokeJson = strokesJson.optJSONObject(i) ?: continue
            val pointsJson = strokeJson.optJSONArray("points") ?: continue
            val points = mutableListOf<GesturePoint>()
            for (j in 0 until pointsJson.length()) {
                val pt = pointsJson.optJSONObject(j) ?: continue
                val px = pt.optIntOrNull("x") ?: continue
                val py = pt.optIntOrNull("y") ?: continue
                points.add(GesturePoint(px, py))
            }
            if (points.isNotEmpty()) {
                strokes.add(GestureStroke(points, strokeJson.optLongOrNull("durationMs") ?: 300L))
            }
        }
        if (strokes.isEmpty()) {
            return buildJsonResponse(400, errorEnvelope("INVALID_ARGUMENT", "At least one valid stroke with points is required."))
        }
        val beforeSnapshot = stateCoordinator.getTreeSnapshotResult().snapshot
        return gestureResponse(
            performed = stateCoordinator.performGesture(strokes),
            beforeSnapshot = beforeSnapshot
        )
    }

    private fun gestureResponse(
        performed: Boolean,
        beforeSnapshot: com.joi.ghosthand.screen.read.AccessibilityTreeSnapshot?
    ): String {
        val observation = if (performed) {
            observeActionSurfaceChange(beforeSnapshot) { stateCoordinator.getTreeSnapshotResult().snapshot }
        } else {
            observeScrollSurfaceChange(beforeSnapshot, beforeSnapshot)
        }
        return if (performed) {
            val actionEffect = observation.toActionEffectObservation()
            val postActionState = PostActionStateComposer.fromObservedEffect(
                actionEffect = actionEffect,
                fallbackSnapshot = observation.afterSnapshot
            )
            observationPublisher.recordActionCompleted(
                route = "/gesture",
                attemptedPath = "gesture_dispatch",
                actionEffect = actionEffect,
                postActionState = postActionState
            )
            buildJsonResponse(
                200,
                successEnvelope(
                    JSONObject(
                        ActionEvidencePayloads.commonFields(
                            performed = true,
                            attemptedPath = "gesture_dispatch",
                            actionEffect = actionEffect,
                            postActionState = postActionState
                        )
                    ),
                    disclosure = buildActionEffectDisclosure("/gesture", true, actionEffect.stateChanged)
                )
            )
        } else {
            buildJsonResponse(422, errorEnvelope("ACCESSIBILITY_ACTION_FAILED", "Gesture dispatch failed."))
        }
    }
}
