package com.example.camp_serve.data

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await

/** Reads and deletes campsite images that were uploaded with [CampsiteImageUpload]. */
interface CampsiteImageStorage {
    /**
     * Downloads an image.
     *
     * @param url download URL saved in the campsite's imageUrl
     * @return the image's bytes
     * @throws Exception if the download fails or the image is over 10 MB
     */
    suspend fun download(url: String): ByteArray

    /**
     * Deletes an image from storage. Does nothing if the file is already gone.
     *
     * @param url download URL saved in the campsite's imageUrl
     * @throws Exception if the delete fails
     */
    suspend fun delete(url: String)
}

private const val MAX_DOWNLOAD_BYTES = 10L * 1024 * 1024

/** Firebase Storage version of [CampsiteImageStorage]. */
class FirebaseCampsiteImageStorage : CampsiteImageStorage {

    override suspend fun download(url: String): ByteArray =
        FirebaseStorage.getInstance().getReferenceFromUrl(url)
            .getBytes(MAX_DOWNLOAD_BYTES).await()

    override suspend fun delete(url: String) {
        try {
            FirebaseStorage.getInstance().getReferenceFromUrl(url).delete().await()
        } catch (e: StorageException) {
            // already deleted, so there is nothing left to remove
            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
        }
    }
}