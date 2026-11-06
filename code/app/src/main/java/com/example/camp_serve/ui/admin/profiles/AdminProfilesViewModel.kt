package com.example.camp_serve.ui.admin.profiles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.camp_serve.data.EntrantRepository
import com.example.camp_serve.data.OwnerRepository
import com.example.camp_serve.data.FirestoreEntrantRepo
import com.example.camp_serve.data.FirestoreOwnerRepo
import com.example.camp_serve.model.Entrant
import com.example.camp_serve.model.Owner
import kotlinx.coroutines.launch

class AdminProfilesViewModel(
    private val entrantRepo: EntrantRepository = FirestoreEntrantRepo(entrantId = ""),
    private val ownerRepo: OwnerRepository = FirestoreOwnerRepo(ownerId = "")
) : ViewModel() {

    var entrants by mutableStateOf<List<Entrant>>(emptyList())
        private set
    var owners by mutableStateOf<List<Owner>>(emptyList())
        private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            entrants = entrantRepo.getAllEntrants()
            owners = ownerRepo.getAllOwners()
        }
    }

    fun removeEntrant(entrant: Entrant) {
        viewModelScope.launch {
            entrantRepo.deleteEntrant(entrant.id)
            entrants = entrantRepo.getAllEntrants()
        }
    }

    fun removeOwner(owner: Owner) {
        viewModelScope.launch {
            ownerRepo.deleteOwner(owner.id)
            owners = ownerRepo.getAllOwners()
        }
    }
}
