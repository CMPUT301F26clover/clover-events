package com.example.camp_serve

import com.example.camp_serve.model.WaitingList
import org.junit.Assert.*
import org.junit.Test

class WaitingListTest {
    @Test
    fun capacityDuplicatesRemovalAndOwnerSnapshot() {
        val list = WaitingList("lottery", "owner", capacity = 1)
        list.addEntrant("first", 100)
        val snapshot = list.getEntrants("owner")
        assertEquals(listOf(WaitingList.Entry("first", 100)), snapshot)
        assertThrows(IllegalStateException::class.java) { list.addEntrant("first", 101) }
        assertThrows(IllegalStateException::class.java) { list.addEntrant("second", 102) }
        assertThrows(IllegalArgumentException::class.java) { list.getEntrants("stranger") }

        assertTrue(list.removeEntrant("first"))
        assertFalse(list.removeEntrant("first"))
        list.addEntrant("second", 103)
        assertEquals("first", snapshot.single().id)
        assertEquals("second", list.getEntrants("owner").single().id)
    }

    @Test
    fun nullCapacityIsUnlimitedAndZeroCapacityRejectsAllJoins() {
        val unlimited = WaitingList("lottery", "owner")
        repeat(100) { unlimited.addEntrant("entrant-$it", it.toLong()) }
        assertEquals(100, unlimited.getEntrants("owner").size)
        assertThrows(IllegalStateException::class.java) {
            WaitingList("lottery", "owner", 0).addEntrant("entrant")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WaitingList("lottery", "owner", -1)
        }
    }
}
