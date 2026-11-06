package com.example.camp_serve.model

/**
 * Registration pool for one lottery. A null [capacity] means unlimited entrants.
 * Timestamps are Unix epoch milliseconds. Returned lists are snapshots.
 */
class WaitingList(
    val lotteryId: String,
    private val ownerId: String,
    capacity: Int? = null
) {
    // Defaults allow Firestore to deserialize entries.
    data class Entry(val id: String = "", val timestamp: Long = 0)

    var capacity: Int? = capacity
        private set

    private val entries = linkedMapOf<String, Entry>()

    init {
        require(lotteryId.isNotBlank()) { "Lottery ID must not be blank." }
        require(ownerId.isNotBlank()) { "Owner ID must not be blank." }
        require(capacity == null || capacity >= 0) { "Capacity must not be negative." }
    }

    @Synchronized
    fun addEntrant(id: String, timestamp: Long = System.currentTimeMillis()) {
        require(id.isNotBlank()) { "Entrant ID must not be blank." }
        check(id !in entries) { "Entrant has already joined." }
        val limit = capacity
        check(limit == null || entries.size < limit) { "Waiting list is full." }
        entries[id] = Entry(id, timestamp)
    }

    @Synchronized
    fun removeEntrant(id: String): Boolean = entries.remove(id) != null

    /** Only the campsite owner can request the full registration list. */
    @Synchronized
    fun getEntrants(requesterId: String): List<Entry> {
        require(requesterId == ownerId) { "Only the owner can view the waiting list." }
        return snapshot()
    }

    @Synchronized
    internal fun snapshot(): List<Entry> = entries.values.toList()

    @Synchronized
    internal fun updateCapacity(capacity: Int?) {
        require(capacity == null || capacity >= entries.size) {
            "Capacity cannot be smaller than the existing waiting list."
        }
        this.capacity = capacity
    }
}
