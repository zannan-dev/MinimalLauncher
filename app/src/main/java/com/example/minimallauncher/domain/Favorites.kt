package com.example.minimallauncher.domain

const val MAX_HOME_FAVORITES = 10

/** Rejects additions at the home limit while always allowing removal. */
fun toggledHomeFavorite(favorites: Set<String>, appKey: String): Set<String> =
    if (appKey !in favorites && favorites.size >= MAX_HOME_FAVORITES) favorites
    else toggledFavorite(favorites, appKey)

/** Adds [appKey] to favorites or removes it when it is already present. */
fun toggledFavorite(favorites: Set<String>, appKey: String): Set<String> =
    if (appKey in favorites) favorites - appKey else favorites + appKey

/** Keeps saved order and includes favorites from older versions that only stored a set. */
fun orderedFavoriteKeys(favorites: Set<String>, savedOrder: List<String>): List<String> =
    savedOrder.distinct().filter { it in favorites } + (favorites - savedOrder.toSet()).sorted()

fun movedFavorite(order: List<String>, fromKey: String, toKey: String): List<String> {
    if (fromKey == toKey || fromKey !in order || toKey !in order) return order
    val result = order.toMutableList()
    val toIndex = result.indexOf(toKey)
    result.remove(fromKey)
    result.add(toIndex, fromKey)
    return result
}
