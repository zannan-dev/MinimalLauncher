package com.example.minimallauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactPhoneTest {
    private val john = ContactPhone(1, "John Smith", "+1 (555) 0100", "Mobile")
    @Test fun completeNameWinsOverPartialMatches() {
        val other = john.copy(contactId = 2, name = "John Smithson")
        assertEquals(listOf(john), selectContactPhones(" JOHN   SMITH ", listOf(other, john)))
    }
    @Test fun duplicateNamesAndMultipleNumbersRemainAmbiguous() {
        val sameName = john.copy(contactId = 2, number = "5550101")
        val work = john.copy(number = "5550102", label = "Work")
        assertEquals(listOf(john, sameName, work), selectContactPhones("John Smith", listOf(john, sameName, work)))
    }
    @Test fun duplicateProviderRowsAreRemovedWithoutMergingPeople() {
        val duplicate = john.copy(number = "+15550100")
        val differentPerson = duplicate.copy(contactId = 2)
        assertEquals(listOf(john, differentPerson), selectContactPhones("John", listOf(john, duplicate, differentPerson)))
    }
    @Test fun missingNumbersDoNotBecomeDialTargets() {
        assertEquals(emptyList<ContactPhone>(), selectContactPhones("John", listOf(john.copy(number = " "), john.copy(number = "---"))))
        assertEquals(emptyList<ContactPhone>(), selectContactPhones("Nobody", emptyList()))
    }
    @Test fun partialMatchesAndExtensionsArePreserved() {
        val extension = john.copy(number = "+15550100;123")
        assertEquals(listOf(john, extension), selectContactPhones("John", listOf(john, extension)))
    }
}
