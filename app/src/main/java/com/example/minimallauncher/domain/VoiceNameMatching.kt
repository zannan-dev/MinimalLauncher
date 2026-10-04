package com.example.minimallauncher.domain

import java.text.Normalizer
import java.util.Locale

private val voiceMarks = Regex("\\p{M}+")
private val voiceSeparators = Regex("[^\\p{L}\\p{N}]+")
fun normalizeVoiceName(name: String): String = Normalizer.normalize(name.lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(voiceMarks, "").replace(voiceSeparators, "")

/** Scores names locally. Approximate matches are suggestions, never automatic actions. */
fun voiceNameScore(query: String, name: String): Int {
    val target = normalizeVoiceName(query)
    val label = normalizeVoiceName(name)
    if (target.isBlank() || label.isBlank()) return 0
    if (target == label) return 100
    val words = name.split(voiceSeparators).filter { it.isNotBlank() }
    if (target.length >= 3 && label.contains(target)) {
        return if (label.startsWith(target) || words.any { normalizeVoiceName(it).startsWith(target) }) 90 else 85
    }
    if (label.length >= 4 && target.contains(label)) return 85
    val forms = (listOf(label) + words.map(::normalizeVoiceName)).distinct()
    return forms.maxOf { form ->
        if (minOf(target.length, form.length) < 3) return@maxOf 0
        val distance = editDistance(target, form)
        val longest = maxOf(target.length, form.length)
        val limit = when { longest >= 8 -> 3; longest >= 5 -> 2; else -> 1 }
        val similarity = 1f - distance.toFloat() / longest
        if (distance <= limit && similarity >= 0.65f) (similarity * 80).toInt()
        else 0
    }
}

fun matchVoiceApps(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
    val ranked = apps.map { it to voiceNameScore(query, it.label) }.filter { it.second > 0 }
        .sortedByDescending { it.second }
    val exact = ranked.filter { it.second == 100 }
    return (exact.ifEmpty { ranked }).map { it.first }
}

private fun editDistance(first: String, second: String): Int {
    var previous = IntArray(second.length + 1) { it }
    first.forEachIndexed { index, character ->
        val current = IntArray(second.length + 1)
        current[0] = index + 1
        second.forEachIndexed { other, value ->
            current[other + 1] = minOf(current[other] + 1, previous[other + 1] + 1,
                previous[other] + if (character == value) 0 else 1)
        }
        previous = current
    }
    return previous.last()
}
