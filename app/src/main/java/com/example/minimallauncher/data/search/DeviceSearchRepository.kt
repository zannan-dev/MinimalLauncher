package com.example.minimallauncher.data.search

import com.example.minimallauncher.domain.DeviceSearchResult

interface DeviceSearchRepository {
    fun settings(query: String): List<DeviceSearchResult>
    suspend fun contacts(query: String): List<DeviceSearchResult>
    fun open(result: DeviceSearchResult): Boolean
}
