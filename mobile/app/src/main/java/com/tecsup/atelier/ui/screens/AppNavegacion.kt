package com.tecsup.atelier.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tecsup.atelier.ui.screens.ia.AiAssistantScreen
import com.tecsup.atelier.ui.screens.inicio.HomeScreen
import com.tecsup.atelier.ui.screens.pedido.OrdersScreen
import com.tecsup.atelier.ui.screens.perfil.ProfileScreen
import com.tecsup.atelier.ui.theme.AtelierTheme

@Composable
fun AppNavegacion() {
    var rutaActual by rememberSaveable { mutableStateOf(Rutas.Inicio.ruta) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Rutas.items().forEach { destino ->
                item(
                    icon = {
                        Icon(
                            painterResource(destino.icono),
                            contentDescription = destino.titulo
                        )
                    },
                    label = { Text(destino.titulo) },
                    selected = rutaActual == destino.ruta,
                    onClick = { rutaActual = destino.ruta }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (rutaActual) {
                Rutas.Inicio.ruta -> HomeScreen(modifier = Modifier.padding(innerPadding))
                Rutas.Pedido.ruta -> OrdersScreen(modifier = Modifier.padding(innerPadding))
                Rutas.IA.ruta -> AiAssistantScreen(modifier = Modifier.padding(innerPadding))
                Rutas.Perfil.ruta -> ProfileScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppNavegacionPreview() {
    AtelierTheme {
        AppNavegacion()
    }
}
