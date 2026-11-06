package com.example.camp_serve.model

/** A saved draw. Redraws create a new result so earlier selections remain in history. */
data class LotteryResult(
    val id: String = "",
    val lotteryId: String = "",
    val winnerIds: List<String> = emptyList(),
    val loserIds: List<String> = emptyList(),
    val drawnAt: Long = 0,
    val drawNumber: Long = 0
)
