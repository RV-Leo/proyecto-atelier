package com.tecsup.atelier.ui.screens.pedido

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.atelier.R
import com.tecsup.atelier.ui.theme.AtelierTheme

data class ItemPedido(
    val idPedido: String,
    val fecha: String,
    val resumenProductos: String,
    val montoTotal: Double,
    val estado: String,
    val pasoEstado: Int, // 1: Confirmado, 2: Preparando, 3: En camino, 4: Entregado
    val colorEstadoHex: Long,
    val coloresProductos: List<Long>
)

val pedidosEjemplo = listOf(
    ItemPedido(
        idPedido = "#AT-8492",
        fecha = "24 de Mayo, 2025",
        resumenProductos = "1x Polo Pima Premium, 1x Pantalón Jean Slim Fit",
        montoTotal = 199.80,
        estado = "En camino",
        pasoEstado = 3,
        colorEstadoHex = 0xFF1976D2,
        coloresProductos = listOf(0xFF2C3E50, 0xFF1B3B6F)
    ),
    ItemPedido(
        idPedido = "#AT-7120",
        fecha = "10 de Mayo, 2025",
        resumenProductos = "1x Casaca Denim Vintage Oversize",
        montoTotal = 189.90,
        estado = "Entregado",
        pasoEstado = 4,
        colorEstadoHex = 0xFF388E3C,
        coloresProductos = listOf(0xFF4B6584)
    ),
    ItemPedido(
        idPedido = "#AT-6501",
        fecha = "28 de Abril, 2025",
        resumenProductos = "2x Polo Graphic Art Streetwear",
        montoTotal = 109.80,
        estado = "Entregado",
        pasoEstado = 4,
        colorEstadoHex = 0xFF388E3C,
        coloresProductos = listOf(0xFF78909C, 0xFF78909C)
    )
)

@Composable
fun OrdersScreen(modifier: Modifier = Modifier) {
    var filtroEstadoSeleccionado by remember { mutableStateOf("Todos") }
    val filtros = listOf("Todos", "En camino", "Entregados")

    val pedidosFiltrados = remember(filtroEstadoSeleccionado) {
        if (filtroEstadoSeleccionado == "Todos") {
            pedidosEjemplo
        } else {
            pedidosEjemplo.filter { it.estado.equals(filtroEstadoSeleccionado, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mis Pedidos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Rastrea y gestiona tus compras textiles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_orders),
                        contentDescription = "Pedidos",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Pestañas de Filtro de Pedidos
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(filtros) { filtro ->
                val esActivo = filtro == filtroEstadoSeleccionado
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (esActivo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { filtroEstadoSeleccionado = filtro }
                ) {
                    Text(
                        text = filtro,
                        color = if (esActivo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (esActivo) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(pedidosFiltrados, key = { it.idPedido }) { pedido ->
                TarjetaPedidoPro(pedido = pedido)
            }
        }
    }
}

@Composable
private fun TarjetaPedidoPro(pedido: ItemPedido) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(pedido.colorEstadoHex))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pedido.idPedido,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = Color(pedido.colorEstadoHex).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = pedido.estado,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(pedido.colorEstadoHex),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = pedido.fecha,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Indicador Timeline de Estado del Pedido
            BarraProgresoEstadoPedido(pasoActual = pedido.pasoEstado)

            Spacer(modifier = Modifier.height(14.dp))

            // Miniaturas de productos comprados
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pedido.coloresProductos.forEach { col ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(col))
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pedido.resumenProductos,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "Total: S/ ${"%.2f".format(pedido.montoTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Volver a Comprar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Rastrear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BarraProgresoEstadoPedido(pasoActual: Int) {
    val pasos = listOf("Confirmado", "Preparando", "En camino", "Entregado")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        pasos.forEachIndexed { idx, paso ->
            val completado = (idx + 1) <= pasoActual
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (completado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (completado) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = paso,
                    fontSize = 9.sp,
                    fontWeight = if (completado) FontWeight.Bold else FontWeight.Medium,
                    color = if (completado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrdersScreenPreview() {
    AtelierTheme {
        OrdersScreen()
    }
}
