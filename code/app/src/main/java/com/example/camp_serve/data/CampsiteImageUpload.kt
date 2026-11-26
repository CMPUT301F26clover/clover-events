package com.example.camp_serve.data

import android.net.Uri
import com.example.camp_serve.model.Campsite
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Uploads campsite images from selected content URIs. */
fun interface CampsiteImageUpload {
    /**
     * Uploads an image and returns its download URL.
     *
     * @param uri content URI of the selected image
     * @return URL that can be saved with the campsite
     * @throws Exception if the upload or URL retrieval fails
     */
    suspend fun upload(uri: String): String
}

/** Stores each campsite image at a unique path in Firebase Storage. */
class FirebaseCampsiteImageUpload : CampsiteImageUpload {
    override suspend fun upload(uri: String): String {
        val ref = FirebaseStorage.getInstance().reference
            .child("campsite-images/${UUID.randomUUID()}")
        ref.putFile(Uri.parse(uri)).await()
        return ref.downloadUrl.await().toString()
    }
}

/**
 * Uploads the optional image, then saves the campsite with its download URL.
 *
 * If no image is selected, the campsite is saved with an empty image URL.
 * An upload failure prevents the campsite from being saved. An image already
 * uploaded is not deleted if the subsequent campsite save fails.
 *
 * @param campsite campsite details to save
 * @param imageUri selected image's content URI, or null when no image was selected
 * @param repository repository used to save the campsite
 * @param imageUpload uploader used when an image was selected
 * @throws Exception if the upload or campsite save fails
 */
suspend fun registerCampsite(
    campsite: Campsite,
    imageUri: String?,
    repository: CampsiteRepository,
    imageUpload: CampsiteImageUpload
) {
    val imageUrl = imageUri?.let { imageUpload.upload(it) } ?: ""
    repository.addCampsite(campsite.copy(imageUrl = imageUrl))
}
