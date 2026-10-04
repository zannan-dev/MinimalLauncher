package com.example.minimallauncher.data.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.example.minimallauncher.domain.DeviceSearchResult
import com.example.minimallauncher.domain.ContactPhone
import com.example.minimallauncher.domain.selectContactPhones
import com.example.minimallauncher.domain.filterSettingsTargets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class AndroidDeviceSearchRepository(context: Context) : DeviceSearchRepository {
    private val context = context.applicationContext
    private val supportedSettings by lazy {
        settingsTargets.filter { Intent(it.action).resolveActivity(this.context.packageManager) != null }
    }

    override fun settings(query: String): List<DeviceSearchResult> {
        if (query.isBlank()) return emptyList()
        return filterSettingsTargets(supportedSettings, query).map {
            DeviceSearchResult("settings:${it.action}:${it.title}", it.title, it.detail, it.action,
                preferenceKey = it.preferenceKey)
        }
    }

    override suspend fun contacts(query: String): List<DeviceSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val matches = linkedMapOf<String, DeviceSearchResult>()
        val namesUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, query.trim())
            .buildUpon().appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY, "20").build()
        context.contentResolver.query(namesUri,
            arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY),
            null, null, "${Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            while (cursor.moveToNext() && matches.size < 20) {
                ensureActive()
                val uri = Contacts.getLookupUri(cursor.getLong(0), cursor.getString(1)) ?: continue
                val key = "contact:$uri"
                matches[key] = DeviceSearchResult(key, cursor.getString(2).orEmpty().ifBlank { "Contact" },
                    "Contact", Intent.ACTION_VIEW, uri.toString())
            }
        }
        val phoneUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, query.trim())
            .buildUpon().appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY, "30").build()
        context.contentResolver.query(phoneUri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                ensureActive()
                val uri = Contacts.getLookupUri(cursor.getLong(0), cursor.getString(1)) ?: continue
                val key = "contact:$uri"
                if (matches.size < 20 || key in matches) {
                    val existing = matches[key]
                    if (existing == null || existing.detail == "Contact") {
                        matches[key] = DeviceSearchResult(key, cursor.getString(2).orEmpty().ifBlank { "Contact" },
                            cursor.getString(3).orEmpty().ifBlank { "Contact" }, Intent.ACTION_VIEW, uri.toString())
                    }
                }
            }
        }
        matches.values.toList()
    }

    override suspend fun contactPhones(query: String): List<ContactPhone> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        selectContactPhones(query, loadVoiceContacts())
    }

    override suspend fun voiceContactNames(): List<String> = withContext(Dispatchers.IO) {
        loadVoiceContacts().map { it.name }.distinct()
    }

    private suspend fun loadVoiceContacts(): List<ContactPhone> {
        val matches = mutableListOf<ContactPhone>()
        // Read the local phone index before matching: provider filtering loses speech spelling variants.
        val uri = Phone.CONTENT_URI
        context.contentResolver.query(uri,
            arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER, Phone.TYPE, Phone.LABEL),
            null, null, "${Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                matches += ContactPhone(cursor.getLong(0), cursor.getString(1).orEmpty().ifBlank { "Contact" },
                    cursor.getString(2).orEmpty(),
                    Phone.getTypeLabel(context.resources, cursor.getInt(3), cursor.getString(4)).toString())
            }
        }
        return matches
    }

    override fun open(result: DeviceSearchResult): Boolean = try {
        context.startActivity(createDeviceSearchIntent(result))
        true
    } catch (_: android.content.ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}


/** Pass both forms used by legacy preference fragments and newer Pixel Settings pages. */
internal fun createDeviceSearchIntent(result: DeviceSearchResult): Intent = Intent(result.action).apply {
    result.uri?.let { data = Uri.parse(it) }
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    result.preferenceKey?.let { key ->
        putExtra(SETTINGS_PREFERENCE_KEY, key)
        putExtra(SETTINGS_FRAGMENT_ARGUMENTS, Bundle().apply { putString(SETTINGS_PREFERENCE_KEY, key) })
        // Recreate an existing standard Settings activity so its previous row cannot win.
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

internal const val SETTINGS_PREFERENCE_KEY = ":settings:fragment_args_key"
internal const val SETTINGS_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

/** Encode the complete number (including extensions) without interpreting it as URI syntax. */
internal fun createContactDialResult(contact: ContactPhone) = DeviceSearchResult(
    "voice:call:${contact.contactId}", contact.name, "", Intent.ACTION_DIAL,
    Uri.fromParts("tel", contact.number, null).toString(),
)
