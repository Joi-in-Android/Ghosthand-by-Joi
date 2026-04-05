/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.routes

import com.joi.ghosthand.screen.find.GhosthandSelectors
import com.joi.ghosthand.screen.find.SelectorQuery
import com.joi.ghosthand.screen.read.TreeUnavailableReason

import com.joi.ghosthand.R

import android.util.Log
import com.joi.ghosthand.payload.GhosthandDisclosure
import com.joi.ghosthand.screen.read.ScreenReadMode
import com.joi.ghosthand.server.LocalApiServerEnvelope
import com.joi.ghosthand.server.LocalApiServer
import org.json.JSONException
import org.json.JSONObject

internal fun successEnvelope(data: JSONObject, disclosure: GhosthandDisclosure? = null): JSONObject {
    return LocalApiServerEnvelope.success(data, disclosure)
}

internal fun errorEnvelope(
    code: String,
    message: String,
    details: JSONObject = JSONObject(),
    disclosure: GhosthandDisclosure? = null
): JSONObject {
    return LocalApiServerEnvelope.error(code, message, details, disclosure)
}

internal fun buildJsonResponse(statusCode: Int, body: JSONObject): String {
    return LocalApiServerEnvelope.httpResponse(statusCode, body)
}

internal fun toJsonValue(value: Any?): Any {
    return LocalApiServerEnvelope.toJsonValue(value)
}

internal fun parseSelector(body: JSONObject): SelectorQuery? {
    val query = body.opt("query").let { value ->
        when {
            value == null || value == JSONObject.NULL -> null
            else -> value.toString()
        }
    }

    return GhosthandSelectors.normalize(
        text = body.optString("text").ifBlank { null },
        desc = body.optString("desc").ifBlank { null },
        id = body.optString("id").ifBlank { null },
        strategy = body.optString("strategy").ifBlank { null },
        query = query
    )
}

internal fun JSONObject.optIntOrNull(key: String): Int? {
    val value = opt(key)
    return if (value is Number) value.toInt() else null
}

internal fun JSONObject.optLongOrNull(key: String): Long? {
    val value = opt(key)
    return if (value is Number) value.toLong() else null
}

internal fun parseJsonBodyOrNull(requestBody: String, endpoint: String): JSONObject? {
    return try {
        JSONObject(requestBody.ifBlank { "{}" })
    } catch (error: JSONException) {
        Log.w(
            LocalApiServer.LOG_TAG,
            "component=LocalApiServer operation=parseJsonBody endpoint=$endpoint failure=${error.javaClass.simpleName}",
            error
        )
        null
    }
}

internal fun badJsonBodyResponse(): String {
    return buildJsonResponse(
        400,
        errorEnvelope("BAD_REQUEST", "Request body must be valid JSON.")
    )
}

internal fun buildTreeUnavailableResponse(reason: TreeUnavailableReason?): String {
    val message = when (reason) {
        TreeUnavailableReason.ACCESSIBILITY_SERVICE_DISCONNECTED ->
            "Accessibility service is unavailable or not connected."
        TreeUnavailableReason.NO_ACTIVE_ROOT ->
            "Accessibility tree is unavailable because no active window root is available."
        null ->
            "Accessibility tree is unavailable."
    }

    return buildJsonResponse(
        statusCode = 503,
        body = errorEnvelope(
            code = "ACCESSIBILITY_UNAVAILABLE",
            message = message
        )
    )
}
