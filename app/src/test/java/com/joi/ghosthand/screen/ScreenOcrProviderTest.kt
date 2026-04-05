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

import com.joi.ghosthand.screen.read.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenOcrProviderTest {
    @Test
    fun providerReturnsEngineElementsWhenScreenshotIsAvailable() {
        val provider = ScreenOcrProvider(
            engine = ScreenOcrEngine {
                ScreenOcrResult(
                    attemptedPath = "ocr_text_recognition",
                    elements = listOf(
                        ScreenReadElement(
                            text = "Hello",
                            bounds = "[0,0][10,10]",
                            centerX = 5,
                            centerY = 5,
                            source = ScreenReadMode.OCR.wireValue
                        )
                    )
                )
            }
        )

        val result = provider.read(
            ScreenshotDispatchResult(
                available = true,
                base64 = "ZmFrZQ==",
                format = "png",
                width = 10,
                height = 10,
                attemptedPath = "accessibility_screenshot"
            )
        )

        assertEquals("ocr_text_recognition", result.attemptedPath)
        assertEquals(1, result.elements.size)
        assertEquals(ScreenReadMode.OCR.wireValue, result.elements.first().source)
    }

    @Test
    fun providerSurfacesUnavailableScreenshotTruth() {
        val provider = ScreenOcrProvider(
            engine = ScreenOcrEngine {
                throw AssertionError("engine should not run when screenshot is unavailable")
            }
        )

        val result = provider.read(
            ScreenshotDispatchResult(
                available = false,
                base64 = null,
                format = "png",
                width = 0,
                height = 0,
                attemptedPath = "service_missing"
            )
        )

        assertEquals("service_missing", result.attemptedPath)
        assertTrue(result.warnings.contains("ocr_screenshot_unavailable"))
        assertTrue(result.elements.isEmpty())
    }
}
