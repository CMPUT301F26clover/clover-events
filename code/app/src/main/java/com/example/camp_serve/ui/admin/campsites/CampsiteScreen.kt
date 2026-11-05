package com.example.camp_serve.ui.admin.campsites

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.camp_serve.R

// This is considered to be the main activity page(for now)
private data class Campsite(
    val name: String,
    val location: String,
    val owner: String,
    val occupancy: Int,
    val price: Double,
    val rating: Double,
)
private val sampleCampsites =
    listOf(
        Campsite("Lakeside Camp", "Banff",  "Alex", 5, 12.99, 4.5),
        Campsite("Pine Ridge", "Jasper", "Sam", 6, 15.99, 4.2)
    )

@Composable
fun CampsitesScreen(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    val campsites = remember { mutableStateListOf(*sampleCampsites.toTypedArray()) }

    val filtered = campsites.filter {
        it.name.contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Campsites",
            fontSize = 30.sp,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            color = Color(0xFF715CAA)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search Campsites") },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        LazyColumn {
            items(filtered) { campsite ->
                CampsiteRow(
                    campsite = campsite,
                    onRemove = { campsites.remove(campsite)}
                )
            }
        }
    }
}

@Composable
private fun CampsiteRow(campsite: Campsite, onRemove : () -> Unit) {
    var removeAlert by remember { mutableStateOf(false) }
    var selectedSite by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF7FF))
            .border(1.dp, Color(0xFF000000))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f) .clickable{ selectedSite = true}) {
            Text("Name: " + campsite.name, fontWeight = FontWeight.Bold)
            Text("Location: " +campsite.location, fontSize = 12.sp)
        }

        if (selectedSite) {
            Dialog(onDismissRequest = { selectedSite = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF7FF),
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(campsite.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Location: " + campsite.location, fontSize = 14.sp)
                        Text("Owner: " + campsite.owner, fontSize = 14.sp)
                        Text("Occupancy: " + campsite.occupancy, fontSize = 12.sp)
                        Text("Price: $" + campsite.price, fontSize = 12.sp)
                        Text("Rating: " +campsite.rating, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { selectedSite = false }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
        Button(onClick = {removeAlert = true}) {
            Text("Remove")
        }

        if (removeAlert) {
            AlertDialog(
                onDismissRequest = { removeAlert = false },
                title = { Text("Remove this campsite?") },
                text = { Text("This removes the listing and it's lottery. This can't be undone") },
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
fun CampsitesScreenPreview() {
    CampsitesScreen(navController = rememberNavController())
}