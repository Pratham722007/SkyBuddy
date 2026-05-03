package com.example.skybuddy.shared.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedRegionManager @Inject constructor() {
    private val _blockedNodeIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedNodeIds: StateFlow<Set<String>> = _blockedNodeIds.asStateFlow()

    fun addBlockedNodes(nodeIds: Set<String>) {
        _blockedNodeIds.value = _blockedNodeIds.value + nodeIds
    }

    /** Replace the entire blocked set — used when receiving a fresh broadcast from security. */
    fun setBlockedNodes(nodeIds: Set<String>) {
        _blockedNodeIds.value = nodeIds
    }

    fun removeBlockedNodes(nodeIds: Set<String>) {
        _blockedNodeIds.value = _blockedNodeIds.value - nodeIds
    }

    fun clearBlockedNodes() {
        _blockedNodeIds.value = emptySet()
    }
}
