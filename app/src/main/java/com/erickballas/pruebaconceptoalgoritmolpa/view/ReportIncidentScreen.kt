package com.erickballas.pruebaconceptoalgoritmolpa.view

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

    var streetId by remember { mutableStateOf("") }
    var incidentType by remember { mutableStateOf("accident") }
    var severity by remember { mutableStateOf(5f) }
    var description by remember { mutableStateOf("") }

    // Usamos los valores recibidos del GPS
    var latitude by remember { mutableStateOf(if (initialLat != 0.0) initialLat.toString() else "") }
    var longitude by remember { mutableStateOf(if (initialLng != 0.0) initialLng.toString() else "") }

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
            Text("Tipo de Incidente", style = MaterialTheme.typography.labelMedium)

            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("accident" to "Accidente", "congestion" to "Tráfico", "road_work" to "Obras").forEach { (type, label) ->
                    FilterChip(
                        selected = incidentType == type,
                        onClick = { incidentType = type },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            OutlinedTextField(
                value = streetId,
                onValueChange = { streetId = it },
                label = { Text("ID Calle (Opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Severidad: ${severity.toInt()}/10")
            Slider(value = severity, onValueChange = { severity = it }, valueRange = 1f..10f, steps = 8)

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Ubicación Detectada:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = latitude, onValueChange = { latitude = it }, label = { Text("Lat") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = longitude, onValueChange = { longitude = it }, label = { Text("Lng") }, modifier = Modifier.weight(1f))
            }

            Button(
                onClick = {
                    val sId = streetId.toIntOrNull() ?: 0
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        viewModel.reportIncident(sId, incidentType, severity.toInt(), lat, lng, description)
                        onIncidentReported()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !incidentState.isLoading
            ) {
                Text(if (incidentState.isLoading) "Enviando..." else "Enviar Reporte")
            }
        }
    }
}