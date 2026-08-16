package com.example.minimallauncher.domain

/** Adds [appKey] to favorites or removes it when it is already present. */
fun toggledFavorite(favorites: Set<String>, appKey: String): Set<String> =
    if (appKey in favorites) favorites - appKey else favorites + appKey
