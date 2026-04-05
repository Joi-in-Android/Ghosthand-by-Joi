/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.screen.find

import com.joi.ghosthand.screen.read.AccessibilityTreeSnapshot
import com.joi.ghosthand.screen.read.AccessibilityTreeSnapshotResult
import com.joi.ghosthand.screen.read.FlatAccessibilityNode

import com.joi.ghosthand.R

import com.joi.ghosthand.payload.GhosthandPayloadJsonSupport
import com.joi.ghosthand.payload.GhosthandScreenPayloads
import org.json.JSONObject

data class FocusedNodeResult(
    val available: Boolean,
    val node: FlatAccessibilityNode?,
    val reason: String?
)

object ScreenFindPayloads {
    fun findPayload(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean,
        index: Int,
        nodeFinder: AccessibilityNodeFinder
    ): JSONObject {
        return GhosthandPayloadJsonSupport.fieldsToJson(
            GhosthandScreenPayloads.findFields(
                findResult(
                    snapshot = snapshot,
                    strategy = strategy,
                    query = query,
                    clickableOnly = clickableOnly,
                    index = index,
                    nodeFinder = nodeFinder
                )
            )
        )
    }

    fun findResult(
        snapshot: AccessibilityTreeSnapshot,
        strategy: String,
        query: String?,
        clickableOnly: Boolean,
        index: Int,
        nodeFinder: AccessibilityNodeFinder
    ): FindNodeResult {
        return nodeFinder.findNodes(
            snapshot = snapshot,
            strategy = strategy,
            query = query,
            clickableOnly = clickableOnly,
            index = index
        )
    }

    fun focusedNodeResult(
        treeResult: AccessibilityTreeSnapshotResult,
        nodeFinder: AccessibilityNodeFinder
    ): FocusedNodeResult {
        if (!treeResult.available || treeResult.snapshot == null) {
            return FocusedNodeResult(
                available = false,
                node = null,
                reason = "accessibility_unavailable"
            )
        }

        val found = nodeFinder.findNode(
            snapshot = treeResult.snapshot,
            strategy = "focused",
            query = null
        )

        return FocusedNodeResult(
            available = true,
            node = found.node,
            reason = null
        )
    }

    fun focusedNodePayload(
        result: FocusedNodeResult,
        nodeToJson: (FlatAccessibilityNode) -> JSONObject
    ): JSONObject {
        return JSONObject()
            .put("available", result.available)
            .put("node", result.node?.let(nodeToJson) ?: JSONObject.NULL)
            .put("reason", result.reason ?: JSONObject.NULL)
    }
}
