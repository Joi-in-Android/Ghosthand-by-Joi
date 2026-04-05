/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.joi.ghosthand.ui.main

import com.joi.ghosthand.capability.CapabilityPolicyStore
import com.joi.ghosthand.capability.GhosthandCapability
import com.joi.ghosthand.integration.github.GitHubReleaseCheckResult
import com.joi.ghosthand.integration.github.GitHubReleaseRepository
import com.joi.ghosthand.ui.permissions.PermissionsScreenUiState
import com.joi.ghosthand.ui.permissions.PermissionsScreenUiStateFactory
import com.joi.ghosthand.state.runtime.RuntimeState
import com.joi.ghosthand.state.runtime.RuntimeStateStore
import com.joi.ghosthand.ui.diagnostics.UpdateUiState
import com.joi.ghosthand.ui.diagnostics.UpdateUiStateFactory

import com.joi.ghosthand.R

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class RuntimeStateViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val capabilityPolicyStore = CapabilityPolicyStore.getInstance(application)
    private val releaseRepository = GitHubReleaseRepository(application)
    private val updateState = MutableLiveData<UpdateUiState>().apply {
        value = UpdateUiStateFactory.fromReleaseCheck(
            GitHubReleaseCheckResult.Checking(releaseRepository.installedAppVersion())
        )
    }

    val runtimeState: LiveData<RuntimeState> = RuntimeStateStore.observe()
    internal val homeScreenState = MediatorLiveData<HomeScreenUiState>().apply {
        fun publish() {
            val runtime = runtimeState.value ?: return
            val update = updateState.value ?: return
            value = HomeScreenUiStateFactory.create(runtime, update)
        }
        addSource(runtimeState) { publish() }
        addSource(updateState) { publish() }
    }
    internal val permissionsScreenState = MediatorLiveData<PermissionsScreenUiState>().apply {
        addSource(runtimeState) { state ->
            value = PermissionsScreenUiStateFactory.create(state)
        }
    }

    init {
        viewModelScope.launch {
            capabilityPolicyStore.observe().collect {
                RuntimeStateStore.refreshRuntimeSnapshot(getApplication())
            }
        }
        refreshReleaseInfo()
    }

    fun requestForegroundServiceStart() {
        RuntimeStateStore.markServiceStartRequested()
    }

    fun setCapabilityPolicy(capability: GhosthandCapability, allowed: Boolean) {
        capabilityPolicyStore.setAllowed(capability, allowed)
    }

    fun recordTapProbeTap(source: String) {
        RuntimeStateStore.markTapProbeTapped(source)
    }

    fun refreshReleaseInfo() {
        updateState.value = UpdateUiStateFactory.fromReleaseCheck(
            GitHubReleaseCheckResult.Checking(releaseRepository.installedAppVersion())
        )
        thread(name = "ghosthand-release-check", isDaemon = true) {
            val result = releaseRepository.checkForUpdate()
            updateState.postValue(UpdateUiStateFactory.fromReleaseCheck(result))
        }
    }
}
