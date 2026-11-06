package com.example.camp_serve.data

import com.example.camp_serve.model.CampsiteOffer

interface ReservationRepository {
    suspend fun getOffer(): CampsiteOffer
    suspend fun reserve(offerId: String, date: String)
    suspend fun decline(offerId: String)
}