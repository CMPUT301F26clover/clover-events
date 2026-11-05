package com.example.camp_serve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.camp_serve.data.TempEntrantRepo
import com.example.camp_serve.ui.admin.AdminApp
import com.example.camp_serve.ui.entrant.profile.ProfileScreen
import com.example.camp_serve.ui.theme.CampserveTheme

class MainActivity : ComponentActivity() {

    private val repository = TempEntrantRepo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampserveTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ProfileScreen(
                        repository = repository,
                        modifier = Modifier.padding(innerPadding)
                    )
                    AdminApp()
                }
            }
        }
    }
}
