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
        compose.onNodeWithText("Joined waitlist").assertDoesNotExist()
        compose.runOnIdle {
            assertTrue(active.getWaitingList(active.ownerId).isEmpty())
            commit.complete(Unit)
        }
        compose.waitUntil(5_000) {
            active.getWaitingList(active.ownerId).isNotEmpty()
        }
        compose.onNodeWithText("Joined waitlist").assertIsNotEnabled()
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
        compose.onNodeWithText("Joined waitlist").assertIsNotEnabled()
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
