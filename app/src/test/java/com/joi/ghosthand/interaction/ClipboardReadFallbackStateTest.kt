/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.interaction

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardReadFallbackStateTest {
    @Test
    fun normalClipboardReadReturnsSystemTextAndClearsFallback() {
        val state = ClipboardReadFallbackState()
        state.recordSuccessfulWrite("ghosthand clip path")

        val liveRead = state.resolveRead(itemCount = 1, text = "system text")
        assertTrue(liveRead.available)
        assertEquals("system text", liveRead.text)
        assertEquals("clipboard_read", liveRead.attemptedPath)

        val nextEmptyRead = state.resolveRead(itemCount = 0, text = null)
        assertFalse(nextEmptyRead.available)
        assertNull(nextEmptyRead.text)
        assertEquals("clipboard_empty", nextEmptyRead.attemptedPath)
    }

    @Test
    fun emptyClipboardFallsBackOnceAfterSuccessfulWrite() {
        val state = ClipboardReadFallbackState()
        state.recordSuccessfulWrite("ghosthand clip path")

        val fallbackRead = state.resolveRead(itemCount = 0, text = null)
        assertTrue(fallbackRead.available)
        assertEquals("ghosthand clip path", fallbackRead.text)
        assertEquals("clipboard_cached_after_write", fallbackRead.attemptedPath)

        val secondEmptyRead = state.resolveRead(itemCount = 0, text = null)
        assertFalse(secondEmptyRead.available)
        assertNull(secondEmptyRead.text)
        assertEquals("clipboard_empty", secondEmptyRead.attemptedPath)
    }

    @Test
    fun emptyClipboardWithoutPriorWriteDoesNotInventText() {
        val state = ClipboardReadFallbackState()

        val result = state.resolveRead(itemCount = 0, text = null)
        assertFalse(result.available)
        assertNull(result.text)
        assertEquals("clipboard_empty", result.attemptedPath)
    }
}
