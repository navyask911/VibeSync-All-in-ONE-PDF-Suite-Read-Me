package com.example.util

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DeepLinkManager {
    private val _pendingBusinessId = MutableStateFlow<String?>(null)
    val pendingBusinessId: StateFlow<String?> = _pendingBusinessId.asStateFlow()

    fun setPendingBusinessId(id: String?) {
        _pendingBusinessId.value = id
    }

    /**
     * Extracts business ID from supported deep links:
     * - vibesync://business?id={businessId}
     * - vibesync://business/{businessId}
     * - https://vibesync.app/biz/{businessId}
     */
    fun parseDeepLinkUri(uri: Uri?): String? {
        if (uri == null) return null
        val scheme = uri.scheme?.lowercase() ?: ""
        val host = uri.host?.lowercase() ?: ""

        if (scheme == "vibesync") {
            if (host == "business" || host == "biz") {
                val queryId = uri.getQueryParameter("id")
                if (!queryId.isNullOrBlank()) {
                    return queryId
                }
                val pathSegment = uri.lastPathSegment
                if (!pathSegment.isNullOrBlank() && pathSegment != "business" && pathSegment != "biz") {
                    return pathSegment
                }
            }
        } else if (scheme == "http" || scheme == "https") {
            if (host.contains("vibesync")) {
                val queryId = uri.getQueryParameter("id")
                if (!queryId.isNullOrBlank()) {
                    return queryId
                }
                val lastSeg = uri.lastPathSegment
                if (!lastSeg.isNullOrBlank() && (uri.path?.contains("/biz") == true || uri.path?.contains("/business") == true)) {
                    return lastSeg
                }
            }
        }
        return null
    }

    fun handleIncomingUri(uri: Uri?) {
        val bizId = parseDeepLinkUri(uri)
        if (!bizId.isNullOrBlank()) {
            _pendingBusinessId.value = bizId
        }
    }
}
