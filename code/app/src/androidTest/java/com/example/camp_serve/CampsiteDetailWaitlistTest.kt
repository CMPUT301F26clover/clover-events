package com.example.camp_serve

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.example.camp_serve.data.DeviceId
import com.example.camp_serve.model.Campsite
import com.example.camp_serve.model.Lottery
import com.example.camp_serve.ui.entrant.campsites.CampsiteDetailScreen
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CampsiteDetailWaitlistTest {
    @get:Rule
    val compose = createComposeRule()

    private fun lottery(start: Long = System.currentTimeMillis() - 60_000,
                        end: Long = System.currentTimeMillis() + 3_600_000) = Lottery(
        id = "lottery-01", ownerId = "fake-owner-01",
        campsites = listOf(Campsite(id = "camp-01", ownerId = "fake-owner-01")),
        registrationStart = start, registrationEnd = end,
        bookingStart = end, bookingEnd = end + 3_600_000, winnerCount = 1
    )

    private fun showScreen(repository: TestLotteryRepository) {
        compose.setContent {
            CampsiteDetailScreen(
                campsiteId = "camp-01",
                navController = rememberNavController(),
                lotteryRepository = repository
            )
        }
    }

    @Test
    fun absentScheduledExpiredAndClosedLotteriesCannotBeJoined() {
        val repository = TestLotteryRepository()
        showScreen(repository)
        compose.onNodeWithText("No active lottery").assertIsDisplayed().assertIsNotEnabled()
        val now = System.currentTimeMillis()
        val inactive = listOf(
            lottery(start = now + 60_000),
            lottery(start = now - 120_000, end = now - 60_000),
            lottery().apply { refreshState(Long.MAX_VALUE) }
        )
        inactive.forEach { candidate ->
            compose.runOnIdle { repository.lotteries.value = listOf(candidate) }
            compose.onNodeWithText("No active lottery").assertIsNotEnabled()
        }
        compose.runOnIdle { assertTrue(repository.joins.isEmpty()) }
    }

    @Test
    fun cancelLeavesTheWaitingListUnchanged() {
        val active = lottery()
        val repository = TestLotteryRepository(listOf(active))
        showScreen(repository)
        compose.onNodeWithText("Join waitlist").assertIsEnabled().performClick()
        compose.onNodeWithText("Join waitlist?").assertIsDisplayed()
        compose.runOnIdle { assertTrue(repository.joins.isEmpty()) }
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Join waitlist?").assertDoesNotExist()
        compose.onNodeWithText("Join waitlist").assertIsEnabled()
        compose.runOnIdle {
            assertTrue(repository.joins.isEmpty())
            assertTrue(active.getWaitingList(active.ownerId).isEmpty())
        }
    }

    @Test
    fun confirmSavesTheDeviceEntrantOnlyAfterCommitAndPreventsRepeatedTaps() {
        val active = lottery()
        val repository = TestLotteryRepository(listOf(active))
        val commit = CompletableDeferred<Unit>()
        repository.commit = commit
        showScreen(repository)
        compose.onNodeWithText("Join waitlist").performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("Joining…").assertIsNotEnabled()
        compose.onNodeWithText("Leave waitlist").assertDoesNotExist()
        compose.runOnIdle {
            assertTrue(active.getWaitingList(active.ownerId).isEmpty())
            commit.complete(Unit)
        }
        compose.waitUntil(5_000) {
            active.getWaitingList(active.ownerId).isNotEmpty()
        }
        compose.onNodeWithText("Leave waitlist").assertIsEnabled()
        compose.runOnIdle {
            val entrantId = DeviceId(InstrumentationRegistry.getInstrumentation().targetContext).get()
            assertEquals(listOf(active.id to entrantId), repository.joins)
            assertEquals(listOf(entrantId), active.getWaitingList(active.ownerId).map { it.id })
        }
    }

    @Test
    fun failedJoinShowsAnErrorAndCanBeRetried() {
        val active = lottery()
        val repository = TestLotteryRepository(listOf(active))
        repository.failure = IllegalStateException("Waiting list is full.")
        showScreen(repository)
        compose.onNodeWithText("Join waitlist").performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("This lottery's waitlist is full.").assertIsDisplayed()
        compose.onNodeWithText("Join waitlist").assertIsEnabled()
        compose.runOnIdle {
            assertTrue(active.getWaitingList(active.ownerId).isEmpty())
            repository.failure = null
        }
        compose.onNodeWithText("Join waitlist").performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("Leave waitlist").assertIsEnabled()
    }

    @Test
    fun existingEntrantCanCancelLeavingThenConfirmRemovalAndRejoin() {
        val entrantId = DeviceId(InstrumentationRegistry.getInstrumentation().targetContext).get()
        val active = lottery().apply {
            join(entrantId)
            join("other-entrant")
        }
        val repository = TestLotteryRepository(listOf(active))
        showScreen(repository)

        // Saved membership must offer leaving even without joining during this screen visit.
        compose.onNodeWithText("Leave waitlist").assertIsEnabled().performClick()
        compose.onNodeWithText("Leave waitlist?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Leave waitlist?").assertDoesNotExist()
        compose.runOnIdle {
            assertTrue(repository.leaves.isEmpty())
            assertTrue(active.isOnWaitingList(entrantId))
        }

        val commit = CompletableDeferred<Unit>()
        compose.runOnIdle { repository.commit = commit }
        compose.onNodeWithText("Leave waitlist").performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("Leaving…").assertIsNotEnabled()
        compose.runOnIdle {
            assertTrue(active.isOnWaitingList(entrantId))
            commit.complete(Unit)
        }
        compose.waitUntil(5_000) { !active.isOnWaitingList(entrantId) }
        compose.onNodeWithText("Join waitlist").assertIsEnabled()
        compose.onNodeWithText("You have left this lottery's waitlist.").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf(active.id to entrantId), repository.leaves)
            assertTrue(active.isOnWaitingList("other-entrant"))
        }

        compose.onNodeWithText("Join waitlist").performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("Leave waitlist").assertIsEnabled()
        compose.runOnIdle {
            assertTrue(active.isOnWaitingList(entrantId))
            assertEquals(listOf(active.id to entrantId), repository.joins)
        }
    }

    @Test
    fun failedLeaveKeepsRegistrationAndAllowsRetry() {
        val entrantId = DeviceId(InstrumentationRegistry.getInstrumentation().targetContext).get()
        val active = lottery().apply { join(entrantId) }
        val repository = TestLotteryRepository(listOf(active))
        repository.failure = IllegalStateException("Save failed")
        showScreen(repository)

        compose.onNodeWithText("Leave waitlist").performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText(
            "Could not leave the waitlist. Check your connection and try again."
        ).assertIsDisplayed()
        compose.onNodeWithText("Leave waitlist").assertIsEnabled()
        compose.runOnIdle {
            assertTrue(active.isOnWaitingList(entrantId))
            repository.failure = null
        }

        compose.onNodeWithText("Leave waitlist").performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("Join waitlist").assertIsEnabled()
        compose.runOnIdle { assertTrue(!active.isOnWaitingList(entrantId)) }
    }

    @Test
    fun registrationEndDismissesLeaveConfirmationWithoutRemovingEntrant() {
        val entrantId = DeviceId(InstrumentationRegistry.getInstrumentation().targetContext).get()
        val repository = TestLotteryRepository(listOf(lottery().apply { join(entrantId) }))
        showScreen(repository)
        compose.onNodeWithText("Leave waitlist").performClick()

        val now = System.currentTimeMillis()
        val expired = lottery(start = now - 120_000, end = now - 60_000).apply {
            join(entrantId, now - 90_000)
        }
        compose.runOnIdle { repository.lotteries.value = listOf(expired) }
        compose.onNodeWithText("Leave waitlist?").assertDoesNotExist()
        compose.onNodeWithText("No active lottery").assertIsNotEnabled()
        compose.runOnIdle {
            assertTrue(repository.leaves.isEmpty())
            assertTrue(expired.isOnWaitingList(entrantId))
        }
    }

    @Test
    fun removingTheActiveLotteryDismissesConfirmationWithoutJoining() {
        val repository = TestLotteryRepository(listOf(lottery()))
        showScreen(repository)
        compose.onNodeWithText("Join waitlist").performClick()
        compose.runOnIdle { repository.lotteries.value = emptyList() }
        compose.onNodeWithText("Join waitlist?").assertDoesNotExist()
        compose.onNodeWithText("No active lottery").assertIsNotEnabled()
        compose.runOnIdle { assertTrue(repository.joins.isEmpty()) }
    }
}
