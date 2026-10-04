package com.example.minimallauncher.data.search

import com.example.minimallauncher.domain.DeviceSearchResult
import com.example.minimallauncher.domain.ContactPhone

interface DeviceSearchRepository {
    fun settings(query: String): List<DeviceSearchResult>
    suspend fun contacts(query: String): List<DeviceSearchResult>
    suspend fun contactPhones(query: String): List<ContactPhone> = emptyList()
    suspend fun voiceContactNames(): List<String> = emptyList()
    fun open(result: DeviceSearchResult): Boolean
}
