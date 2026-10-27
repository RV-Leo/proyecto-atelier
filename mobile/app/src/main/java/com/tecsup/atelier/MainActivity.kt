package com.tecsup.atelier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tecsup.atelier.ui.screens.AppNavegacion
import com.tecsup.atelier.ui.theme.AtelierTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtelierTheme {
                AppNavegacion()
            }
        }
    }
}
