package com.tecsup.atelier.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.atelier.ui.modelo.Producto
import com.tecsup.atelier.ui.screens.detalle.DetalleProductoScreen
import com.tecsup.atelier.ui.screens.ia.AiAssistantScreen
import com.tecsup.atelier.ui.screens.inicio.HomeScreen
import com.tecsup.atelier.ui.screens.pedido.OrdersScreen
import com.tecsup.atelier.ui.screens.perfil.ProfileScreen
import com.tecsup.atelier.ui.theme.AtelierTheme

@Composable
fun AppNavegacion() {
    var rutaActual by rememberSaveable { mutableStateOf(Rutas.Inicio.ruta) }
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (productoSeleccionado == null) {
                FlotanteBarraNavegacion(
                    rutaActual = rutaActual,
                    alSeleccionarRuta = { rutaActual = it }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (productoSeleccionado != null) {
                DetalleProductoScreen(
                    producto = productoSeleccionado!!,
                    alVolver = { productoSeleccionado = null },
                    alAgregarAlCarrito = { _, _, _ ->
                        productoSeleccionado = null
                    }
                )
            } else {
                when (rutaActual) {
                    Rutas.Inicio.ruta -> HomeScreen(
                        alSeleccionarProducto = { producto ->
                            productoSeleccionado = producto
                        }
                    )
                    Rutas.Pedido.ruta -> OrdersScreen()
                    Rutas.IA.ruta -> AiAssistantScreen()
                    Rutas.Perfil.ruta -> ProfileScreen()
                }
            }
        }
    }
}

@Composable
private fun FlotanteBarraNavegacion(
    rutaActual: String,
    alSeleccionarRuta: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Rutas.items().forEach { destino ->
                    val estaSeleccionado = rutaActual == destino.ruta
                    
                    val colorFondoCapsula by animateColorAsState(
                        targetValue = if (estaSeleccionado) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f) else Color.Transparent,
                        label = "colorFondo"
                    )
                    
                    val escalaIcono by animateFloatAsState(
                        targetValue = if (estaSeleccionado) 1.1f else 1.0f,
                        label = "escala"
                    )

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colorFondoCapsula,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { alSeleccionarRuta(destino.ruta) }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(destino.icono),
                                contentDescription = destino.titulo,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(escalaIcono)
                            )
                            
                            if (estaSeleccionado) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = destino.titulo,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
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
