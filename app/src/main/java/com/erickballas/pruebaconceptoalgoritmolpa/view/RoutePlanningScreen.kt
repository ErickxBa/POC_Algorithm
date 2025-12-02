package com.erickballas.pruebaconceptoalgoritmolpa.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.RouteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanningScreen(
    viewModel: RouteViewModel,
    currentUserLat: Double = 0.0,
    currentUserLng: Double = 0.0,
    onBackClick: () -> Unit = {},
    onRouteCalculated: () -> Unit = {}
) {
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    var destinationQuery by remember { mutableStateOf("") }
    var destLat by remember { mutableStateOf(0.0) }
    var destLng by remember { mutableStateOf(0.0) }
    var safetyProfile by remember { mutableStateOf("balanced") }
    var isSearching by remember { mutableStateOf(false) }

    // Color Azul Oscuro (Figma)
    val darkBlue = Color(0xFF1A237E)

    Column(modifier = Modifier.fillMaxSize().background(darkBlue)) {

        // CORRECCIÓN: Componente actualizado de Material 3
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                navigationIconContentColor = Color.White
            )
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ingresa la Ruta", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))

            // Input Origen
            OutlinedTextField(
                value = "Tu ubicación", onValueChange = {}, readOnly = true,
                leadingIcon = { Icon(Icons.Default.MyLocation, null, tint = Color.Black) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFE0E0E0), unfocusedContainerColor = Color(0xFFE0E0E0), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input Destino
            OutlinedTextField(
                value = destinationQuery,
                onValueChange = { destinationQuery = it; isSearching = true; viewModel.searchLocation(it) },
                placeholder = { Text("¿A dónde quieres ir?") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFFE0E0E0), unfocusedContainerColor = Color(0xFFE0E0E0), focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
            )

            // Sugerencias
            if (isSearching && suggestions.isNotEmpty()) {
                Card(modifier = Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(max = 200.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    LazyColumn {
                        items(suggestions) { place ->
                            ListItem(
                                headlineContent = { Text(place.display_name, color = Color.Black) },
                                leadingContent = { Icon(Icons.Default.LocationOn, null, tint = Color.Gray) },
                                modifier = Modifier.clickable {
                                    destinationQuery = place.display_name.take(30) + "..."
                                    destLat = place.lat.toDoubleOrNull() ?: 0.0
                                    destLng = place.lon.toDoubleOrNull() ?: 0.0
                                    isSearching = false
                                    viewModel.clearSuggestions()
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.White)
                            )
                            Divider(color = Color.LightGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Perfil
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("fastest" to "Rápida", "balanced" to "Balance", "safest" to "Segura").forEach { (id, label) ->
                    FilterChip(
                        selected = safetyProfile == id, onClick = { safetyProfile = id },
                        label = { Text(label, color = if(safetyProfile==id) Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, containerColor = Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (destLat != 0.0) {
                        viewModel.calculateRouteFromCoordinates(currentUserLat, currentUserLng, destLat, destLng, safetyProfile)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !routeState.isLoading && destLat != 0.0,
                shape = RoundedCornerShape(24.dp)
            ) {
                if (routeState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("VER RUTA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Resultado
            if (routeState.route != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("✅ ¡Ruta Encontrada!", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Distancia: ${(routeState.route?.totalDistance ?: 0.0) / 1000} km", color = Color.Gray)
                        // Este botón regresa al mapa para que veas la ruta pintada
                        Button(
                            onClick = { onRouteCalculated() },
                            modifier = Modifier.fillMaxWidth().padding(top=8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = darkBlue)
                        ) { Text("Ver en Mapa") }
                    }
                }
            }
            if (routeState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Error: ${routeState.error}", color = Color.Red)
            }
        }
    }
}