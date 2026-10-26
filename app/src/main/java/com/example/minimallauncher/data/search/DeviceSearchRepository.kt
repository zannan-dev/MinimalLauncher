package com.example.minimallauncher.data.search

import com.example.minimallauncher.domain.DeviceSearchResult
import com.example.minimallauncher.domain.RecentSearchResult

interface DeviceSearchRepository {
    fun settings(query: String): List<DeviceSearchResult>
    suspend fun contacts(query: String): List<DeviceSearchResult>
    suspend fun resolveRecent(result: RecentSearchResult): DeviceSearchResult?
    fun open(result: DeviceSearchResult): Boolean
}
