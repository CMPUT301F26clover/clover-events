package com.example.camp_serve

import com.example.camp_serve.ui.owner.campsites.isValidLatitude
import com.example.camp_serve.ui.owner.campsites.isValidLongitude
import com.example.camp_serve.ui.owner.campsites.isValidOccupancy
import com.example.camp_serve.ui.owner.campsites.isValidPrice
import com.example.camp_serve.ui.owner.campsites.isValidText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * CampsiteValidationTest.kt
 * Unit tests for the checks on the register campsite form (US 02.01.01).
 */
class CampsiteValidationTest {

    @Test
    fun textWithWordsIsValid() {
        assertTrue(isValidText("Maligne Ridge"))
    }

    @Test
    fun emptyTextIsNotValid() {
        assertFalse(isValidText(""))
        assertFalse(isValidText("   "))
    }

    @Test
    fun coordinatesAcceptValidValuesAndBoundaries() {
        listOf("-90", "0", " 53.5461 ", "90").forEach { assertTrue(isValidLatitude(it)) }
        listOf("-180", "0", "-113.4938", "180").forEach { assertTrue(isValidLongitude(it)) }
    }

    @Test
    fun coordinatesRejectInvalidValues() {
        listOf("", "north", "-90.1", "90.1", "NaN", "Infinity").forEach {
            assertFalse(isValidLatitude(it))
        }
        listOf("", "west", "-180.1", "180.1", "NaN", "-Infinity").forEach {
            assertFalse(isValidLongitude(it))
        }
    }

    @Test
    fun wholeNumberOccupancyIsValid() {
        assertTrue(isValidOccupancy("1"))
        assertTrue(isValidOccupancy("8"))
    }

    @Test
    fun badOccupancyIsNotValid() {
        assertFalse(isValidOccupancy("0"))
        assertFalse(isValidOccupancy("-3"))
        assertFalse(isValidOccupancy("abc"))
        assertFalse(isValidOccupancy("2.5"))
    }

    @Test
    fun normalPriceIsValid() {
        assertTrue(isValidPrice("65"))
        assertTrue(isValidPrice("49.99"))
        assertTrue(isValidPrice("0"))
    }

    @Test
    fun badPriceIsNotValid() {
        assertFalse(isValidPrice("-10"))
        assertFalse(isValidPrice("free"))
    }
}