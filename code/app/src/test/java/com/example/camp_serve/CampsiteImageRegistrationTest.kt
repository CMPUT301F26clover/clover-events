package com.example.camp_serve

import com.example.camp_serve.data.CampsiteImageUpload
import com.example.camp_serve.data.CampsiteRepository
import com.example.camp_serve.data.TempCampsiteRepo
import com.example.camp_serve.data.registerCampsite
import com.example.camp_serve.model.Campsite
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CampsiteImageRegistrationTest {
    private val campsite = Campsite(ownerId = "owner", name = "Lake")

    @Test
    fun selectedImageIsUploadedAndUrlSaved() = runBlocking {
        val repo = TempCampsiteRepo()
        var uploadedUri = ""
        val uploader = CampsiteImageUpload { uri ->
            uploadedUri = uri
            "https://example.com/image"
        }

        registerCampsite(campsite, "content://image/1", repo, uploader)

        assertEquals("content://image/1", uploadedUri)
        assertEquals("https://example.com/image", repo.getCampsitesForOwner("owner").single().imageUrl)
    }

    @Test
    fun noImageSavesWithoutUploading() = runBlocking {
        val repo = TempCampsiteRepo()
        registerCampsite(campsite, null, repo, CampsiteImageUpload { error("Unexpected upload") })
        assertEquals("", repo.getCampsitesForOwner("owner").single().imageUrl)
    }

    @Test
    fun uploadFailureDoesNotSaveCampsite() = runBlocking {
        val repo = TempCampsiteRepo()
        try {
            registerCampsite(campsite, "content://image/1", repo, CampsiteImageUpload { error("Upload failed") })
            fail("Expected upload failure")
        } catch (e: IllegalStateException) {
            assertEquals("Upload failed", e.message)
        }
        assertTrue(repo.getCampsitesForOwner("owner").isEmpty())
    }

    @Test
    fun repositoryFailurePropagates() = runBlocking {
        val repo = object : CampsiteRepository by TempCampsiteRepo() {
            override suspend fun addCampsite(campsite: Campsite) {
                error("Save failed")
            }
        }
        try {
            registerCampsite(campsite, "content://image/1", repo, CampsiteImageUpload { "https://example.com/image" })
            fail("Expected save failure")
        } catch (e: IllegalStateException) {
            assertEquals("Save failed", e.message)
        }
    }
}
