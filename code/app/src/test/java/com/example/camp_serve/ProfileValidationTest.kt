package com.example.camp_serve

import com.example.camp_serve.ui.entrant.profile.isValidEmail
import com.example.camp_serve.ui.entrant.profile.isValidPhone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the profile validation functions (US 01.07.03).
 */
class ProfileValidationTest {

    @Test
    fun validEmail_isAccepted() {
        assertTrue(isValidEmail("test@example.ca"))
    }

    @Test
    fun emailWithSpacesAround_isAccepted() {
        assertTrue(isValidEmail("  test@example.ca  "))
    }

    @Test
    fun emptyEmail_isRejected() {
        assertFalse(isValidEmail(""))
    }

    @Test
    fun emailWithoutAtSign_isRejected() {
        assertFalse(isValidEmail("testexample.ca"))
    }

    @Test
    fun emailWithoutDomainDot_isRejected() {
        assertFalse(isValidEmail("test@example"))
    }

    @Test
    fun emptyPhone_isAccepted() {
        assertTrue(isValidPhone(""))
    }

    @Test
    fun validPhone_isAccepted() {
        assertTrue(isValidPhone("780-123-4567"))
    }

    @Test
    fun phoneWithCountryCode_isAccepted() {
        assertTrue(isValidPhone("+1 (780) 123-4567"))
    }

    @Test
    fun tooShortPhone_isRejected() {
        assertFalse(isValidPhone("12345"))
    }

    @Test
    fun phoneWithLetters_isRejected() {
        assertFalse(isValidPhone("780-123-ABCD"))
    }
}