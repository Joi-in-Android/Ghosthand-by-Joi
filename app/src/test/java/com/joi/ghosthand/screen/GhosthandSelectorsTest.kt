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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GhosthandSelectorsTest {
    @Test
    fun normalizePrefersTextAliasFirst() {
        val selector = GhosthandSelectors.normalize(
            text = "Send",
            desc = "ignored",
            id = "ignored",
            strategy = "contentDesc",
            query = "ignored"
        )

        assertEquals(SelectorQuery("text", "Send"), selector)
    }

    @Test
    fun normalizeMapsDescAndIdAliasesToInternalStrategies() {
        assertEquals(
            SelectorQuery("contentDesc", "Search"),
            GhosthandSelectors.normalize(
                text = null,
                desc = "Search",
                id = null,
                strategy = null,
                query = null
            )
        )
        assertEquals(
            SelectorQuery("resourceId", "android:id/input"),
            GhosthandSelectors.normalize(
                text = null,
                desc = null,
                id = "android:id/input",
                strategy = null,
                query = null
            )
        )
    }

    @Test
    fun normalizeFallsBackToExplicitStrategyAndQuery() {
        val selector = GhosthandSelectors.normalize(
            text = null,
            desc = null,
            id = null,
            strategy = "textContains",
            query = "wifi"
        )

        assertEquals(SelectorQuery("textContains", "wifi"), selector)
    }

    @Test
    fun normalizeReturnsNullWhenNoSelectorProvided() {
        assertNull(
            GhosthandSelectors.normalize(
                text = null,
                desc = null,
                id = null,
                strategy = null,
                query = null
            )
        )
    }
}
