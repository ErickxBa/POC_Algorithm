package com.erickballas.pruebaconceptoalgoritmolpa.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.IncidentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(
    viewModel: IncidentsViewModel,
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
    onBackClick: () -> Unit = {},
    onIncidentReported: () -> Unit = {}
) {
    val incidentState by viewModel.incidentsState.collectAsStateWithLifecycle()
    val suggestions by viewModel.searchSuggestions.collectAsStateWithLifecycle()

    var streetId by remember { mutableStateOf("") }
    var incidentType by remember { mutableStateOf("accident") }
    var severity by remember { mutableStateOf(5f) }
    var description by remember { mutableStateOf("") }

    // Ubicación (se puede editar manualmente o via búsqueda)
    var latitude by remember { mutableStateOf(if (initialLat != 0.0) initialLat.toString() else "") }
    var longitude by remember { mutableStateOf(if (initialLng != 0.0) initialLng.toString() else "") }

    // Buscador
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar Incidente") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // --- SECCIÓN DE BÚSQUEDA ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    isSearching = true
                    viewModel.searchLocation(it)
                },
                label = { Text("Buscar sitio o dirección...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            // Lista de sugerencias (Solo visible si hay resultados y estamos buscando)
            if (isSearching && suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    LazyColumn {
                        items(suggestions) { place ->
                            ListItem(
                                headlineContent = { Text(place.display_name, maxLines = 1) },
                                leadingContent = { Icon(Icons.Default.Place, null) },
                                modifier = Modifier.clickable {
                                    // AL SELECCIONAR:
                                    latitude = place.lat
                                    longitude = place.lon
                                    searchQuery = place.display_name.take(30) + "..." // Mostrar nombre corto
                                    isSearching = false // Ocultar lista
                                    viewModel.clearSuggestions()
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
            // ---------------------------

            Text("Tipo de Incidente", style = MaterialTheme.typography.labelMedium)

            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("accident" to "Accidente", "congestion" to "Tráfico", "road_work" to "Obras", "robbery" to "Robo").forEach { (type, label) ->
                    FilterChip(
                        selected = incidentType == type,
                        onClick = { incidentType = type },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Text("Ubicación Seleccionada:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitud") },
                    modifier = Modifier.weight(1f),
                    readOnly = true // Mejor solo lectura si usamos búsqueda/mapa
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitud") },
                    modifier = Modifier.weight(1f),
                    readOnly = true
                )
            }

            Text("Severidad: ${severity.toInt()}/10")
            Slider(value = severity, onValueChange = { severity = it }, valueRange = 1f..10f, steps = 8)

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (Opcional)") },
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val sId = streetId.toIntOrNull() ?: 0 // Opcional
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        viewModel.reportIncident(sId, incidentType, severity.toInt(), lat, lng, description)
                        onIncidentReported()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !incidentState.isLoading && latitude.isNotEmpty()
            ) {
                Text(if (incidentState.isLoading) "Enviando..." else "Confirmar Reporte")
            }
        }
    }
}