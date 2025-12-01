package com.erickballas.pruebaconceptoalgoritmolpa.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.IncidentsViewModel

/**
 * Pantalla para reportar incidentes
 */
@Composable
fun ReportIncidentScreen(
    viewModel: IncidentsViewModel,
    onBackClick: () -> Unit = {},
    onIncidentReported: () -> Unit = {}
) {
    val incidentState by viewModel.incidentsState.collectAsStateWithLifecycle()
    
    var streetId by remember { mutableStateOf("1") }
    var incidentType by remember { mutableStateOf("accident") }
    var severity by remember { mutableStateOf(5f) }
    var description by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("10.3932") }
    var longitude by remember { mutableStateOf("-75.4830") }

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
            // Tipo de incidente
            Text(
                "Tipo de Incidente",
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "accident" to "🚗 Accidente",
                    "congestion" to "🚦 Congestión",
                    "road_work" to "🔧 Obra",
                    "hazard" to "⚠️ Peligro"
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = incidentType == type,
                        onClick = { incidentType = type },
                        label = { Text(label) }
                    )
                }
            }

            // Calle/Segmento
            Text(
                "Calle/Segmento",
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = streetId,
                onValueChange = { streetId = it },
                label = { Text("ID de la calle") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Severidad
            Text(
                "Severidad: ${severity.toInt()}/10",
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = severity,
                onValueChange = { severity = it },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )

            // Descripción
            Text(
                "Descripción",
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Detalles del incidente...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4
            )

            // Ubicación
            Text(
                "Ubicación (Latitud, Longitud)",
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitud") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitud") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón reportar
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = {
                    val street = streetId.toIntOrNull()
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()
                    val sev = severity.toInt()
                    
                    if (street != null && lat != null && lng != null) {
                        viewModel.reportIncident(
                            streetId = street,
                            incidentType = incidentType,
                            severity = sev,
                            latitude = lat,
                            longitude = lng,
                            description = description
                        )
                        onIncidentReported()
                    }
                },
                enabled = !incidentState.isLoading
            ) {
                if (incidentState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Reportar Incidente")
                }
            }

            // Resultado
            incidentState.lastReportId?.let { reportId ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        "✅ Incidente reportado\nID: $reportId",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Error
            incidentState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        "❌ Error: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
