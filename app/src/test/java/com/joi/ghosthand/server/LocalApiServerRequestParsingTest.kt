/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.server

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

import com.joi.ghosthand.TestFileSupport
import com.joi.ghosthand.payload.*
import com.joi.ghosthand.routes.read.screenSummaryOnlyRequested
import com.joi.ghosthand.server.LocalApiServerProtocol
import com.joi.ghosthand.server.LocalApiServerResources
import com.joi.ghosthand.server.LocalApiServerRoute
import com.joi.ghosthand.server.LocalApiServerRouteRegistry
import com.joi.ghosthand.server.LocalApiServerRequestException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LocalApiServerRequestParsingTest {
    @Test
    fun parseInputRequestRejectsClearActionWhenTextIsAlsoProvided() {
        val parsed = GhosthandApiPayloads.parseInputRequest(
            mapOf(
                "textAction" to "clear",
                "text" to "should-not-be-here"
            )
        )

        assertEquals("text must be omitted when textAction=clear.", parsed.errorMessage)
        assertEquals(null, parsed.request)
    }

    @Test
    fun parseInputRequestAcceptsExplicitTextAndEnterSequence() {
        val parsed = GhosthandApiPayloads.parseInputRequest(
            mapOf(
                "textAction" to "set",
                "text" to "wifi",
                "key" to "enter"
            )
        )

        assertNull(parsed.errorMessage)
        assertEquals(InputTextAction.SET, parsed.request?.textAction)
        assertEquals("wifi", parsed.request?.text)
        assertEquals(InputKey.ENTER, parsed.request?.key)
    }

    @Test
    fun parseSelectorSupportsTextAliasInsideWaitCondition() {
        val selector = GhosthandSelectors.normalize(
            text = "XYZdefinitelydoesnotexist999",
            desc = null,
            id = null,
            strategy = "text",
            query = null
        )

        assertEquals("text", selector?.strategy)
        assertEquals("XYZdefinitelydoesnotexist999", selector?.query)
    }

    @Test
    fun screenSummaryOnlyRequestedDetectsExplicitOptIn() {
        assertTrue(screenSummaryOnlyRequested(mapOf("summaryOnly" to "true")))
        assertFalse(screenSummaryOnlyRequested(mapOf("summaryOnly" to "false")))
        assertFalse(screenSummaryOnlyRequested(emptyMap()))
    }

    @Test
    fun screenPreviewThumbOptInHelperIsRemovedFromReadRouteHandlers() {
        val routeHandlers = TestFileSupport.readProjectFile(
            "app/src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenRouteHandlers.kt",
            "src/main/java/com/folklore25/ghosthand/routes/read/ReadScreenRouteHandlers.kt"
        )

        assertFalse(routeHandlers.contains("screenPreviewThumbRequested"))
        assertFalse(routeHandlers.contains("includePreview"))
    }

    @Test
    fun selectorNormalizationLeavesNonFocusedStrategyQueryNullWhenAliasMissing() {
        val selector = GhosthandSelectors.normalize(
            text = null,
            desc = null,
            id = null,
            strategy = "text",
            query = null
        )

        assertEquals("text", selector?.strategy)
        assertNull(selector?.query)
    }

    @Test
    fun readRequestParsesValidRequest() {
        val input = ByteArrayInputStream(
            (
                "POST /tap?package=com.example HTTP/1.1\r\n" +
                    "Content-Length: 7\r\n" +
                    "\r\n" +
                    "{\"x\":1}"
                ).toByteArray(StandardCharsets.UTF_8)
        )

        val request = LocalApiServerProtocol.readRequest(input)

        assertEquals("POST", request.method)
        assertEquals("/tap", request.path)
        assertEquals("com.example", request.queryParameters["package"])
        assertEquals("{\"x\":1}", request.body)
    }

    @Test
    fun readRequestRejectsMalformedHeaderLine() {
        val input = ByteArrayInputStream(
            "GET /health HTTP/1.1\r\nBroken-Header\r\n\r\n".toByteArray(StandardCharsets.UTF_8)
        )

        val error = expectParseFailure {
            LocalApiServerProtocol.readRequest(input)
        }

        assertEquals(400, error.statusCode)
        assertEquals("BAD_REQUEST", error.errorCode)
    }

    @Test
    fun readRequestRejectsInvalidContentLength() {
        val input = ByteArrayInputStream(
            (
                "POST /tap HTTP/1.1\r\n" +
                    "Content-Length: nope\r\n" +
                    "\r\n"
                ).toByteArray(StandardCharsets.UTF_8)
        )

        val error = expectParseFailure {
            LocalApiServerProtocol.readRequest(input)
        }

        assertEquals(400, error.statusCode)
        assertEquals("BAD_REQUEST", error.errorCode)
    }

    @Test
    fun readRequestRejectsOversizedBodyBeforeAllocation() {
        val body = "abcdef"
        val input = ByteArrayInputStream(
            (
                "POST /tap HTTP/1.1\r\n" +
                    "Content-Length: ${body.length}\r\n" +
                    "\r\n" +
                    body
                ).toByteArray(StandardCharsets.UTF_8)
        )

        val error = expectParseFailure {
            LocalApiServerProtocol.readRequest(input, maxBodyBytes = 5)
        }

        assertEquals(413, error.statusCode)
        assertEquals("REQUEST_TOO_LARGE", error.errorCode)
    }

    @Test
    fun stopAllReleasesServerClientsAndExecutors() {
        val serverExecutor = Executors.newSingleThreadExecutor()
        val clientExecutor = Executors.newFixedThreadPool(1)
        val resources = LocalApiServerResources(
            serverExecutor = serverExecutor,
            clientExecutor = clientExecutor
        )
        val serverSocket = TrackingServerSocket()
        val firstClient = TrackingSocket()
        val secondClient = TrackingSocket()

        resources.attachServerSocket(serverSocket)
        resources.registerClient(firstClient)
        resources.registerClient(secondClient)

        resources.stopAll()

        assertTrue(serverSocket.closed)
        assertTrue(firstClient.closed)
        assertTrue(secondClient.closed)
        assertTrue(serverExecutor.isShutdown)
        assertTrue(clientExecutor.isShutdown)
        assertTrue(resources.awaitStopped(1, TimeUnit.SECONDS))
        assertFalse(resources.hasActiveClients())
    }

    @Test
    fun routeRegistryDispatchesMatchingMethodAndPath() {
        val registry = LocalApiServerRouteRegistry(
            routes = listOf(
                LocalApiServerRoute("GET", "/screen") { "matched" }
            ),
            pathPolicies = emptyMap()
        )

        val response = registry.dispatch(
            method = "GET",
            path = "/screen",
            queryParameters = emptyMap(),
            requestBody = ""
        )

        assertEquals("matched", response)
    }

    private fun expectParseFailure(block: () -> Unit): LocalApiServerRequestException {
        try {
            block()
        } catch (error: LocalApiServerRequestException) {
            return error
        }

        fail("Expected LocalApiServerRequestException")
        throw AssertionError("unreachable")
    }

    private class TrackingSocket : Socket() {
        var closed = false

        override fun close() {
            closed = true
        }
    }

    private class TrackingServerSocket : ServerSocket() {
        var closed = false

        override fun close() {
            closed = true
        }
    }
}
