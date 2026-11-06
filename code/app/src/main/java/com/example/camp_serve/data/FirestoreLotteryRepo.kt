package com.example.camp_serve.data

import com.example.camp_serve.model.Lottery
import com.example.camp_serve.model.LotteryResult
import com.example.camp_serve.model.WaitingList
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Standard Firestore storage:
 * - lotteries/{id}: configuration, state, and current selection
 * - waitingLists/{lotteryId}: capacity and timestamped registrations
 * - lotteryResults/{resultId}: immutable history of each draw/redraw
 *
 * Mutations read fresh documents in a transaction, so joins cannot overfill a
 * list or overwrite another device's join. Notifications run outside transactions.
 * Owner checks here complement, rather than replace, deployed Firestore rules.
 */
class FirestoreLotteryRepo(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val clock: () -> Long = System::currentTimeMillis
) : LotteryRepository {
    private fun lotteryDoc(id: String) = db.collection("lotteries").document(validId(id))
    private fun waitingListDoc(id: String) = db.collection("waitingLists").document(validId(id))
    private fun results() = db.collection("lotteryResults")

    private fun validId(id: String): String {
        require(id.isNotBlank() && '/' !in id && id != "." && id != "..") {
            "A document ID must be nonblank and must not contain a slash."
        }
        return id
    }

    override suspend fun createLottery(lottery: Lottery) {
        val documents = LotteryDocuments.capture(lottery)
        require(documents.lottery.state == Lottery.State.OPEN.name && documents.lottery.drawNumber == 0L) {
            "Create a lottery before drawing it."
        }
        val lotteryRef = lotteryDoc(lottery.id)
        val waitingRef = waitingListDoc(lottery.id)
        db.runTransaction { transaction ->
            val existingLottery = transaction.get(lotteryRef)
            val existingWaitingList = transaction.get(waitingRef)
            check(!existingLottery.exists() && !existingWaitingList.exists()) {
                "Lottery already exists."
            }
            transaction.set(lotteryRef, documents.lottery)
            transaction.set(waitingRef, documents.waitingList)
        }.await()
    }

    override suspend fun getLottery(id: String): Lottery? =
        db.runTransaction { transaction -> read(transaction, id) }.await()

    override suspend fun updateLottery(lottery: Lottery, requesterId: String) {
        val candidate = LotteryDocuments.capture(lottery).lottery
        db.runTransaction { transaction ->
            val current = checkNotNull(read(transaction, lottery.id)) { "Lottery not found." }
            require(candidate.ownerId == current.ownerId) { "The owner cannot be changed." }
            current.update(
                requesterId = requesterId,
                campsites = candidate.campsites,
                registrationStart = candidate.registrationStart,
                registrationEnd = candidate.registrationEnd,
                bookingStart = candidate.bookingStart,
                bookingEnd = candidate.bookingEnd,
                winnerCount = candidate.winnerCount,
                waitlistCapacity = candidate.waitlistCapacity,
                now = clock()
            )
            val documents = LotteryDocuments.capture(current)
            transaction.set(lotteryDoc(current.id), documents.lottery)
            transaction.set(waitingListDoc(current.id), documents.waitingList)
        }.await()
    }

    override suspend fun joinLottery(id: String, entrantId: String) {
        db.runTransaction { transaction ->
            val current = checkNotNull(read(transaction, id)) { "Lottery not found." }
            current.join(entrantId, clock())
            transaction.set(waitingListDoc(id), LotteryDocuments.capture(current).waitingList)
        }.await()
    }

    override suspend fun leaveLottery(id: String, entrantId: String): Boolean =
        db.runTransaction { transaction ->
            val current = checkNotNull(read(transaction, id)) { "Lottery not found." }
            val removed = current.leave(entrantId, clock())
            if (removed) {
                transaction.set(waitingListDoc(id), LotteryDocuments.capture(current).waitingList)
            }
            removed
        }.await()

    override suspend fun getWaitingList(id: String, requesterId: String): List<WaitingList.Entry> =
        checkNotNull(getLottery(id)) { "Lottery not found." }.getWaitingList(requesterId)

    override suspend fun getResults(lotteryId: String): List<LotteryResult> =
        results().whereEqualTo("lotteryId", validId(lotteryId)).get().await()
            .toObjects(LotteryResult::class.java).sortedBy { it.drawNumber }

    override suspend fun advance(id: String, random: Random): LotteryRepository.Progress {
        // Generate once, not on each transaction retry.
        val resultRef = results().document()
        return db.runTransaction { transaction ->
            val lottery = checkNotNull(read(transaction, id)) { "Lottery not found." }
            val previousState = lottery.state
            val now = clock()
            val selection = lottery.drawIfReady(now, random)
            val result = selection?.let {
                LotteryResult(resultRef.id, id, it.first, it.second, now, lottery.drawNumber)
            }
            if (result != null) {
                transaction.set(lotteryDoc(id), LotteryDocuments.capture(lottery).lottery)
                transaction.set(resultRef, result)
            } else if (previousState != lottery.state) {
                transaction.update(lotteryDoc(id), "state", lottery.state.name)
            }
            LotteryRepository.Progress(lottery, result)
        }.await()
    }

    override suspend fun redraw(
        id: String,
        requesterId: String,
        random: Random
    ): LotteryRepository.Progress {
        val resultRef = results().document()
        return db.runTransaction { transaction ->
            val lottery = checkNotNull(read(transaction, id)) { "Lottery not found." }
            val now = clock()
            val selection = lottery.redraw(requesterId, now, random)
            val result = LotteryResult(
                resultRef.id, id, selection.first, selection.second, now, lottery.drawNumber
            )
            transaction.set(lotteryDoc(id), LotteryDocuments.capture(lottery).lottery)
            transaction.set(resultRef, result)
            LotteryRepository.Progress(lottery, result)
        }.await()
    }

    override fun observeChanges(id: String): Flow<Unit> = callbackFlow {
        val listener = lotteryDoc(id).addSnapshotListener { _, error ->
            if (error != null) close(error) else trySend(Unit)
        }
        awaitClose { listener.remove() }
    }.buffer(Channel.CONFLATED)

    private fun read(transaction: Transaction, id: String): Lottery? {
        // All reads occur before writes, including in the caller's transaction.
        val lotterySnapshot = transaction.get(lotteryDoc(id))
        val waitingSnapshot = transaction.get(waitingListDoc(id))
        if (!lotterySnapshot.exists()) {
            check(!waitingSnapshot.exists()) { "Waiting list has no lottery." }
            return null
        }
        val lottery = checkNotNull(lotterySnapshot.toObject(LotteryDocument::class.java))
        val waitingList = checkNotNull(waitingSnapshot.toObject(WaitingListDocument::class.java)) {
            "Lottery is missing its waiting list."
        }
        return LotteryDocuments(lottery, waitingList).restore(id)
    }
}
