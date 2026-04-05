/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.preview

import com.joi.ghosthand.integration.projection.MediaProjectionProvider
import com.joi.ghosthand.interaction.execution.ScreenshotDispatchResult

import com.joi.ghosthand.R

import android.media.projection.MediaProjection
import com.joi.ghosthand.interaction.execution.GhosthandScreenshotAccess

internal class ScreenPreviewCoordinator(
    private val screenshotAccess: GhosthandScreenshotAccess,
    private val mediaProjectionProvider: MediaProjectionProvider
) {
    fun setMediaProjection(projection: MediaProjection) {
        mediaProjectionProvider.setProjection(projection)
    }

    fun hasMediaProjection(): Boolean = mediaProjectionProvider.hasProjection()

    fun capturePreview(displayWidth: Int, displayHeight: Int): ScreenPreviewCapture {
        return ScreenPreviewCaptureSupport.capturePreview(
            displayWidth = displayWidth,
            displayHeight = displayHeight,
            captureScreenshot = ::captureBestScreenshot
        )
    }

    fun captureBestScreenshot(width: Int, height: Int): ScreenshotDispatchResult {
        return screenshotAccess.captureBestAvailable(
            width = width,
            height = height,
            captureProjection = mediaProjectionProvider::captureScreenshot
        )
    }
}
