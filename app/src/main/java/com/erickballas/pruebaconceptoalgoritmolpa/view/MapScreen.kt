package com.erickballas.pruebaconceptoalgoritmolpa.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapViewModel

/**
 * Pantalla principal de Mapa con Google Maps
 * Muestra:
 * - Ubicación del usuario
 * - Rutas calculadas
 * - Incidentes cercanos
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onIncidentClick: (String) -> Unit = {}
) {
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    
    // Ubicación inicial (Cartagena, Colombia)
    val initialLocation = LatLng(10.3932, -75.4830)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 15f)
    }

    var showIncidentDialog by remember { mutableStateOf(false) }
    var selectedIncidentId by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Mapa
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true,
                mapToolbarEnabled = true
            )
        ) {
            // Marcador de ubicación del usuario
            mapState.userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Tu ubicación",
                    infoWindow = {
                        Text("Estás aquí", style = MaterialTheme.typography.labelMedium)
                    }
                )
            }

            // Dibujar ruta como polilínea
            if (mapState.route.isNotEmpty()) {
                Polyline(
                    points = mapState.route,
                    color = Color.Blue,
                    width = 8f,
                    geodesic = true
                )
                
                // Marcador de inicio (verde)
                mapState.route.firstOrNull()?.let { start ->
                    Marker(
                        state = MarkerState(position = start),
                        title = "Inicio",
                        snippet = "Punto de salida"
                    )
                }
                
                // Marcador de fin (rojo)
                mapState.route.lastOrNull()?.let { end ->
                    Marker(
                        state = MarkerState(position = end),
                        title = "Destino",
                        snippet = "Punto de llegada"
                    )
                }
            }

            // Mostrar incidentes como marcadores
            mapState.incidents.forEach { incident ->
                val color = when (incident.severity) {
                    in 1..3 -> Color.Yellow       // Bajo
                    in 4..6 -> Color(0xFFFFA500)  // Naranja (Medio)
                    else -> Color.Red             // Alto
                }
                
                Marker(
                    state = MarkerState(position = incident.location),
                    title = incident.type.uppercase(),
                    snippet = "Severidad: ${incident.severity}/10",
                    onClick = {
                        selectedIncidentId = incident.id
                        showIncidentDialog = true
                        onIncidentClick(incident.id)
                        true
                    }
                )
            }
        }

        // Panel superior con búsqueda
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            SearchBar(
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Indicador de carga
            if (mapState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }

            // Mensaje de error
            mapState.error?.let { error ->
                Text(
                    text = "⚠️ Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Panel inferior con controles
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp)
        ) {
            // Info de incidentes
            if (mapState.incidents.isNotEmpty()) {
                Text(
                    text = "📍 ${mapState.incidents.size} incidentes cercanos",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Botones de control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = { viewModel.clearRoute() }
                ) {
                    Text("Limpiar")
                }

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = {
                        mapState.userLocation?.let { location ->
                            cameraPositionState.animate(
                                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                    location,
                                    15f
                                )
                            )
                        }
                    }
                ) {
                    Text("Mi Ubicación")
                }
            }
        }
    }

    // Diálogo de detalles de incidente
    if (showIncidentDialog) {
        val incident = mapState.incidents.find { it.id == selectedIncidentId }
        incident?.let {
            IncidentDetailDialog(
                incident = it,
                onDismiss = { showIncidentDialog = false }
            )
        }
    }
}

@Composable
fun SearchBar(
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    
    OutlinedTextField(
        value = searchText,
        onValueChange = { searchText = it },
        modifier = modifier.height(48.dp),
        placeholder = { Text("Buscar ubicación...") },
        singleLine = true,
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { searchText = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                }
            }
        },
        shape = MaterialTheme.shapes.small
    )
}

@Composable
fun IncidentDetailDialog(
    incident: com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapIncident,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("📌 Incidente Reportado")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Tipo: ${incident.type.uppercase()}")
                Text("Severidad: ${incident.severity}/10")
                Text("Lat: ${String.format("%.4f", incident.location.latitude)}")
                Text("Lng: ${String.format("%.4f", incident.location.longitude)}")
                
                // Barra de severidad
                LinearProgressIndicator(
                    progress = { incident.severity / 10f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when (incident.severity) {
                        in 1..3 -> Color.Yellow
                        in 4..6 -> Color(0xFFFFA500)
                        else -> Color.Red
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
