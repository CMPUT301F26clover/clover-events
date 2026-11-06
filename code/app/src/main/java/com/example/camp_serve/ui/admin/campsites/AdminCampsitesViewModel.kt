package com.example.camp_serve.ui.admin.campsites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.model.Campsite
import kotlinx.coroutines.launch

class AdminCampsitesViewModel(
    private val repo: CampsiteRepository = TempCampsiteRepo()
) : ViewModel() {

    var campsites by mutableStateOf<List<Campsite>>(emptyList())
        private set

    init { load() }

    fun load() {
        viewModelScope.launch { campsites = repo.getAllCampsites() }
    }

    fun remove(campsite: Campsite) {
        viewModelScope.launch {
            repo.deleteCampsite(campsite.id)
            campsites = repo.getAllCampsites()
        }
    }


}