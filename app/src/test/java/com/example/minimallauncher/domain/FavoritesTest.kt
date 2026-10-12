package com.example.minimallauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesTest {
    @Test
    fun `tenth home favorite is allowed but eleventh is rejected`() {
        val nine = (1..9).map { "app$it" }.toSet()
        val ten = toggledHomeFavorite(nine, "app10")
        assertEquals(nine + "app10", ten)
        assertEquals(ten, toggledHomeFavorite(ten, "app11"))
    }

    @Test
    fun `removing a favorite at the limit frees a slot`() {
        val ten = (1..10).map { "app$it" }.toSet()
        val removed = toggledHomeFavorite(ten, "app1")
        assertEquals(ten - "app1" + "app11", toggledHomeFavorite(removed, "app11"))
    }

    @Test
    fun `legacy oversized favorites allow removal but reject additions`() {
        val legacy = (1..12).map { "app$it" }.toSet()
        assertEquals(legacy, toggledHomeFavorite(legacy, "app13"))
        assertEquals(legacy - "app1", toggledHomeFavorite(legacy, "app1"))
    }

    @Test
    fun `other app selections remain unlimited`() {
        val selected = (1..10).map { "app$it" }.toSet()
        assertTrue("app11" in toggledFavorite(selected, "app11"))
    }
}
