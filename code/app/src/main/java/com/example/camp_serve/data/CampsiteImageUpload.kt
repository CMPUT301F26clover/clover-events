package com.example.camp_serve.data

import android.net.Uri
import com.example.camp_serve.model.Campsite
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Uploads a selected content URI and returns a persistent download URL. */
fun interface CampsiteImageUpload {
    suspend fun upload(uri: String): String
}

class FirebaseCampsiteImageUpload : CampsiteImageUpload {
    override suspend fun upload(uri: String): String {
        val ref = FirebaseStorage.getInstance().reference
            .child("campsite-images/${UUID.randomUUID()}")
        ref.putFile(Uri.parse(uri)).await()
        return ref.downloadUrl.await().toString()
    }
}

suspend fun registerCampsite(
    campsite: Campsite,
    imageUri: String?,
    repository: CampsiteRepository,
    imageUpload: CampsiteImageUpload
) {
    val imageUrl = imageUri?.let { imageUpload.upload(it) } ?: ""
    repository.addCampsite(campsite.copy(imageUrl = imageUrl))
}
