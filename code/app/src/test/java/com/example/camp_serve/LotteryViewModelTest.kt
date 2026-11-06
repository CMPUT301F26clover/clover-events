package com.example.camp_serve

import com.example.camp_serve.data.TempLotteryRepo
import com.example.camp_serve.ui.owner.lottery.LotteryManagementViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LotteryViewModelTest {
    private lateinit var viewModel: LotteryManagementViewModel
    private lateinit var repo: TempLotteryRepo

    @Before
    fun setUp() {
        repo = TempLotteryRepo()
        viewModel = LotteryManagementViewModel(repo, "camp-01")
    }

    @Test
    fun testInvalidWinnerCountFails() = runBlocking {
        val success = viewModel.saveLottery("2026-06-01", "2026-06-05", "2026-06-06", "2026-06-10", "0")
        assertFalse(success)
        assertNotNull(viewModel.errorMessage.value)
    }

    @Test
    fun testBookingBeforeRegistrationEndsFails() = runBlocking {
        val success = viewModel.saveLottery("2026-06-01", "2026-06-10", "2026-06-05", "2026-06-15", "2")
        assertFalse(success)
        assertEquals("Booking period cannot start before the registration period ends.", viewModel.errorMessage.value)
    }
}