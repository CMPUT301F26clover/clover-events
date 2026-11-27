package com.example.camp_serve.ui.admin.images

import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.camp_serve.data.CampsiteImageStorage
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.FirebaseCampsiteImageStorage
import com.example.camp_serve.data.FirestoreCampsiteRepo
import com.example.camp_serve.data.FirestoreOwnerRepo
import com.example.camp_serve.data.OwnerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One uploaded image in the admin list. Images are campsite photos, so the
 * uploading user is the campsite's owner.
 */
data class AdminImageItem(
    val campsiteId: String,
    val campsiteName: String,
    val ownerName: String,
    val imageUrl: String
)

class AdminImagesViewModel(
    private val campsiteRepo: CampsiteRepository = FirestoreCampsiteRepo(),
    private val ownerRepo: OwnerRepository = FirestoreOwnerRepo(ownerId = ""),
    private val storage: CampsiteImageStorage = FirebaseCampsiteImageStorage()
) : ViewModel() {

    private companion object {
        // Downsample so a list of large photos doesn't use too much memory.
        const val MAX_SIDE_PX = 1024
    }

    var images by mutableStateOf<List<AdminImageItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** One-time message for the screen to show, e.g. "Image removed". */
    var message by mutableStateOf<String?>(null)
        private set

    private val bitmapCache = mutableMapOf<String, ImageBitmap>()

    init { load() }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            try {
                val owners = ownerRepo.getAllOwners().associate { it.id to it.name }
                images = campsiteRepo.getAllCampsites()
                    .filter { it.imageUrl.isNotBlank() }
                    .map { c ->
                        AdminImageItem(
                            campsiteId = c.id,
                            campsiteName = c.name,
                            ownerName = owners[c.ownerId]?.ifBlank { c.ownerId } ?: c.ownerId,
                            imageUrl = c.imageUrl
                        )
                    }
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Could not load images"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Deletes the stored file first, so a failed delete leaves everything unchanged,
     * then clears the campsite's imageUrl so entrants and owners no longer see it.
     */
    fun remove(item: AdminImageItem) {
        viewModelScope.launch {
            try {
                storage.delete(item.imageUrl)
                campsiteRepo.getCampsite(item.campsiteId)?.let {
                    campsiteRepo.updateCampsite(it.copy(imageUrl = ""))
                }
                bitmapCache.remove(item.imageUrl)
                images = images.filterNot { it.campsiteId == item.campsiteId }
                message = "Image removed"
            } catch (e: Exception) {
                message = "Couldn't remove image: ${e.message}"
            }
        }
    }

    fun messageShown() {
        message = null
    }

    /** Downloads and decodes an image, or returns null if that fails. */
    suspend fun loadImage(url: String): ImageBitmap? {
        bitmapCache[url]?.let { return it }
        return try {
            val bytes = storage.download(url)
            val bitmap = withContext(Dispatchers.Default) { decode(bytes) }
            bitmap?.also { bitmapCache[url] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun decode(bytes: ByteArray): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    }
}