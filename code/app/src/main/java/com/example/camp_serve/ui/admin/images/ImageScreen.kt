package com.example.camp_serve.ui.admin.images

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.camp_serve.R

@Composable
fun ImagesScreen(navController: NavController, vm: AdminImagesViewModel = viewModel()) {
    ImagesContent(
        images = vm.images,
        isLoading = vm.isLoading,
        error = vm.error,
        message = vm.message,
        onMessageShown = { vm.messageShown() },
        onRemove = { vm.remove(it) },
        loadImage = { vm.loadImage(it) }
    )
}

@Composable
private fun ImagesContent(
    images: List<AdminImageItem>,
    isLoading: Boolean,
    error: String?,
    message: String?,
    onMessageShown: () -> Unit,
    onRemove: (AdminImageItem) -> Unit,
    loadImage: suspend (String) -> ImageBitmap?
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show the message, then clear it so it isn't shown again.
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Images",
                fontSize = 30.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                color = Color(0xFF715CAA)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                when {
                    isLoading -> item { StatusText("Loading images...") }
                    error != null -> item { StatusText("Couldn't load images: $error") }
                    images.isEmpty() -> item { StatusText("No images have been uploaded.") }
                    else -> items(images) { image ->
                        ImageRow(
                            image = image,
                            onRemove = { onRemove(image) },
                            loadImage = loadImage
                        )
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text, modifier = Modifier.padding(16.dp))
}

/** Shows the downloaded image, or the placeholder while loading or if it fails. */
@Composable
private fun CampsiteImage(
    url: String,
    loadImage: suspend (String) -> ImageBitmap?,
    contentScale: ContentScale,
    modifier: Modifier
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = url) {
        value = loadImage(url)
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.blank_profile_picture),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

@Composable
private fun ImageRow(
    image: AdminImageItem,
    onRemove: () -> Unit,
    loadImage: suspend (String) -> ImageBitmap?
) {
    var removeAlert by remember { mutableStateOf(false) }
    var imageEnlarge by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF7FF))
            .border(1.dp, Color(0xFF000000))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CampsiteImage(
            url = image.imageUrl,
            loadImage = loadImage,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { imageEnlarge = true }
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(image.campsiteName, fontWeight = FontWeight.Bold)
            Text("Uploaded by: " + image.ownerName, fontSize = 12.sp)
        }

        Button(onClick = { removeAlert = true }) {
            Text("Remove")
        }
    }

    if (imageEnlarge) {
        Dialog(onDismissRequest = { imageEnlarge = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFEF7FF)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CampsiteImage(
                        url = image.imageUrl,
                        loadImage = loadImage,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { imageEnlarge = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }

    if (removeAlert) {
        AlertDialog(
            onDismissRequest = { removeAlert = false },
            title = { Text("Remove this Image?") },
            text = { Text("This removes the campsite image. This can't be undone") },
            confirmButton = {
                TextButton(onClick = {
                    removeAlert = false
                    onRemove()
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { removeAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ImagesScreenPreview() {
    ImagesContent(
        images = listOf(
            AdminImageItem("1", "Banff Ridge Camp", "Sam Sample", "https://example.com/1.jpg"),
            AdminImageItem("2", "Maligne Ridge", "Alex Sample", "https://example.com/2.jpg")
        ),
        isLoading = false,
        error = null,
        message = null,
        onMessageShown = {},
        onRemove = {},
        loadImage = { null }
    )
}