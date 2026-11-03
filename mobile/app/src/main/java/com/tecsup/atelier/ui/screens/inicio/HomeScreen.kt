package com.tecsup.atelier.ui.screens.inicio

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.atelier.R
import com.tecsup.atelier.data.repository.RepositorioCatalogoImpl
import com.tecsup.atelier.ui.modelo.CarritoManager
import com.tecsup.atelier.ui.modelo.Categoria
import com.tecsup.atelier.ui.modelo.Producto
import com.tecsup.atelier.ui.theme.AtelierTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    alSeleccionarProducto: (Producto) -> Unit = {},
    alAbrirCarrito: () -> Unit = {}
) {
    var idCategoriaSeleccionada by remember { mutableStateOf("all") }
    var textoBusqueda by remember { mutableStateOf("") }
    var productos by remember { mutableStateOf<List<Producto>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorApi by remember { mutableStateOf<String?>(null) }
    var intento by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val repositorio = remember { RepositorioCatalogoImpl() }

    LaunchedEffect(intento) {
        cargando = true
        errorApi = null
        repositorio.listarPrendas(pagina = 1, tamano = 100)
            .onSuccess { pagina ->
                productos = pagina.items
                cargando = false
            }
            .onFailure { fallo ->
                productos = emptyList()
                errorApi = fallo.message ?: "No se pudo conectar con la API."
                cargando = false
            }
    }

    val categorias = remember(productos) {
        val tipos = productos
            .map { Categoria(it.idCategoria, it.nombreCategoria) }
            .distinctBy { it.id }
            .sortedBy { it.nombre }
        listOf(Categoria("all", "Todos")) + tipos
    }

    LaunchedEffect(categorias) {
        if (categorias.none { it.id == idCategoriaSeleccionada }) {
            idCategoriaSeleccionada = "all"
        }
    }

    val favoritosState = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(productos) {
        productos.forEach { producto ->
            if (favoritosState[producto.id] == null) {
                favoritosState[producto.id] = producto.esFavorito
            }
        }
    }

    val productosFiltrados = remember(productos, idCategoriaSeleccionada, textoBusqueda) {
        val consulta = textoBusqueda.trim()
        productos.filter { producto ->
            val coincideCategoria = idCategoriaSeleccionada == "all" ||
                    producto.idCategoria == idCategoriaSeleccionada
            val coincideBusqueda = consulta.isBlank() ||
                    listOf(
                        producto.nombre,
                        producto.nombreCategoria,
                        producto.tela,
                        producto.estilo,
                        producto.colores,
                        producto.descripcion
                    ).any { it.contains(consulta, ignoreCase = true) }
            coincideCategoria && coincideBusqueda
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Encabezado Editorial Minimalista
            item(span = { GridItemSpan(2) }) {
                EncabezadoEditorialMinimalista(
                    textoBusqueda = textoBusqueda,
                    alCambiarTextoBusqueda = { textoBusqueda = it },
                    alAbrirCarrito = alAbrirCarrito
                )
            }

            // 2. Banner Héroe con color de marca consistente
            item(span = { GridItemSpan(2) }) {
                BannerHeroeMinimalista()
            }

            // 3. Categorías (Pill Tabs) derivadas de los tipos de prenda de la API
            item(span = { GridItemSpan(2) }) {
                Column {
                    Text(
                        text = "Categorías",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categorias, key = { it.id }) { categoria ->
                            TabCategoriaMinimalista(
                                categoria = categoria,
                                estaSeleccionado = categoria.id == idCategoriaSeleccionada,
                                alSeleccionar = { idCategoriaSeleccionada = categoria.id }
                            )
                        }
                    }
                }
            }

            // 4. Título de Catálogo
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (idCategoriaSeleccionada == "all") {
                            "Colección Exclusiva"
                        } else {
                            "Categoría: ${categorias.firstOrNull { it.id == idCategoriaSeleccionada }?.nombre ?: idCategoriaSeleccionada}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            cargando -> "Cargando..."
                            errorApi != null -> "Sin conexión"
                            else -> "${productosFiltrados.size} prendas"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 5. Contenido del catálogo
            when {
                cargando -> {
                    item(span = { GridItemSpan(2) }) {
                        EstadoCargandoCatalogo()
                    }
                }

                errorApi != null -> {
                    item(span = { GridItemSpan(2) }) {
                        EstadoErrorCatalogo(
                            mensaje = errorApi!!,
                            alReintentar = { intento++ }
                        )
                    }
                }

                productosFiltrados.isEmpty() -> {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron prendas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    items(productosFiltrados, key = { it.id }) { producto ->
                        val esFav = favoritosState[producto.id] ?: false
                        TarjetaProductoMinimalista(
                            producto = producto,
                            esFavorito = esFav,
                            alAlternarFavorito = { favoritosState[producto.id] = !esFav },
                            alAgregarAlCarrito = {
                                CarritoManager.agregarProducto(producto)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Añadido al carrito: ${producto.nombre}")
                                }
                            },
                            alHacerClic = { alSeleccionarProducto(producto) }
                        )
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun EstadoCargandoCatalogo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Cargando prendas desde la API...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EstadoErrorCatalogo(
    mensaje: String,
    alReintentar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No pudimos cargar el catálogo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Verifica que el backend esté encendido en la URL configurada.\n$mensaje",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = alReintentar, shape = RoundedCornerShape(14.dp)) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun EncabezadoEditorialMinimalista(
    textoBusqueda: String,
    alCambiarTextoBusqueda: (String) -> Unit,
    alAbrirCarrito: () -> Unit
) {
    val itemsCarrito by CarritoManager.items.collectAsState()
    val cantidadItems = itemsCarrito.sumOf { it.cantidad }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "A T E L I E R",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Boutique Textil Peruana",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón Carrito con Contador Badge
                Box(modifier = Modifier.clickable(onClick = alAbrirCarrito)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cart),
                                contentDescription = "Carrito",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (cantidadItems > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$cantidadItems",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Avatar usuario
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "CA",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = textoBusqueda,
            onValueChange = alCambiarTextoBusqueda,
            placeholder = { Text("Buscar prendas, telas o estilos...") },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BannerHeroeMinimalista() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E232A)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = Color(0xFFC68A4C),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "CATÁLOGO ATELIER",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Prendas creadas con IA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Explora el catálogo que publica la API de Atelier",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .size(44.dp)
                    .clickable { }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "→",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E232A),
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TabCategoriaMinimalista(
    categoria: Categoria,
    estaSeleccionado: Boolean,
    alSeleccionar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (estaSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = alSeleccionar)
    ) {
        Text(
            text = categoria.nombre,
            color = if (estaSeleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun TarjetaProductoMinimalista(
    producto: Producto,
    esFavorito: Boolean,
    alAlternarFavorito: () -> Unit,
    alAgregarAlCarrito: () -> Unit,
    alHacerClic: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alHacerClic)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color(producto.colorHex))
            ) {
                if (producto.esNuevo) {
                    Surface(
                        color = Color(0xFFC68A4C),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "NUEVO",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clickable(onClick = alAlternarFavorito)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(if (esFavorito) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline),
                            contentDescription = "Favorito",
                            tint = if (esFavorito) Color(0xFFE53935) else Color.DarkGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = producto.nombreCategoria.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (producto.calificacion > 0) {
                        Text(
                            text = "★ ${producto.calificacion}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (producto.tela.isNotBlank()) {
                    Text(
                        text = "Tela: ${producto.tela}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (producto.tienePrecio) {
                            Text(
                                text = "S/ ${"%.2f".format(producto.precio)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            producto.precioOriginal?.let { orig ->
                                Text(
                                    text = "S/ ${"%.2f".format(orig)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        } else {
                            Text(
                                text = "Precio por confirmar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable(onClick = alAgregarAlCarrito)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = "Agregar",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AtelierTheme {
        HomeScreen()
    }
}
