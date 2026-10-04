package com.example.minimallauncher.domain

data class ContactPhone(val contactId: Long, val name: String, val number: String, val label: String)

/** Prefer a complete name, but keep every distinct person and number for the chooser. */
fun selectContactPhones(query: String, phones: List<ContactPhone>): List<ContactPhone> {
    val usable = phones.filter { it.number.any(Char::isDigit) }.distinctBy {
        it.contactId to it.number.filter { character -> character.isDigit() || character == '+' || character == ',' || character == ';' || character == '*' || character == '#' }
    }
    val ranked = usable.map { it to voiceNameScore(query, it.name) }.filter { it.second > 0 }
        .sortedByDescending { it.second }
    val exact = ranked.filter { it.second == 100 }
    return (exact.ifEmpty { ranked }).map { it.first }
}
