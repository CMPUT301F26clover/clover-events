package com.example.camp_serve.ui.admin.images

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import com.example.camp_serve.R


private data class ImageItem(
    val name: String,
    var type: Int
    // val image: URL
)
private val sampleImages =
    listOf(
        ImageItem("Banff Ridge Camp", 1),
        ImageItem("Banff National Park", 1),
        ImageItem("George Smith", 2),
        ImageItem("John Doe", 2),
        ImageItem("Ashley Johnson", 2),
    )

@Composable
fun ImagesScreen(navController: NavController) {
    val images = remember { mutableStateListOf(*sampleImages.toTypedArray()) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Images",
            fontSize = 30.sp,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            color = Color(0xFF715CAA)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(images) { campsite ->
                ImageRow(
                    images= campsite,
                    onRemove = {images.remove(campsite)}
                )
            }
        }
    }
}

@Composable
private fun ImageRow(images: ImageItem, onRemove : () -> Unit) {
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
        Image(
            painter = painterResource(id = R.drawable.blank_profile_picture),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { imageEnlarge = true }
        )

        if (imageEnlarge) {
            Dialog(onDismissRequest = { imageEnlarge = false }) {
                Image( // https://developer.android.com/codelabs/basic-android-kotlin-compose-add-images#2
                    painter = painterResource(id = R.drawable.blank_profile_picture),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { imageEnlarge = false }) {
                    Text("Close")
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            if(images.type == 1) {
                Text(images.name + " Photo", fontWeight = FontWeight.Bold)
            }
            else {
                Text(images.name + " Profile Picture", fontWeight = FontWeight.Bold)
            }
        }
        Button(onClick = {removeAlert = true}) {
            Text("Remove")
        }

        if (removeAlert) {
            AlertDialog(
                onDismissRequest = { removeAlert = false },
                title = { Text("Remove this Image?") },
                text = {
                    if (images.type == 1) {
                        Text("This removes the the campsite image. This can't be undone")
                    }
                    else {
                        Text("This removes the profile image. This can't be undone")
                    }
                       },
                confirmButton = {
                    TextButton(onClick = {
                        removeAlert = false
                        onRemove()
                    }) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        removeAlert = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ImagesScreenPreview() {
    ImagesScreen(navController = rememberNavController())
}