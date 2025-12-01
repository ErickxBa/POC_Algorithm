package com.erickballas.pruebaconceptoalgoritmolpa.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.RouteViewModel
import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Pantalla para calcular rutas
 * Permite seleccionar:
 * - Nodo inicial
 * - Nodo destino
 * - Perfil de seguridad (fastest, balanced, safest)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanningScreen(
    viewModel: RouteViewModel,
    onBackClick: () -> Unit = {},
    onRouteCalculated: () -> Unit = {}
) {
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()
    
    var startNodeId by remember { mutableStateOf("100") }
    var goalNodeId by remember { mutableStateOf("500") }
    var selectedProfile by remember { mutableStateOf("balanced") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calcular Ruta") },
                navigationIcon = {
                    Button(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.textButtonColors()
                    ) {
                        Text("← Atrás")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selector de nodo inicial
            Text(
                "Nodo Inicial",
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = startNodeId,
                onValueChange = { startNodeId = it },
                label = { Text("Ej: 100, 200, 300...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Selector de nodo destino
            Text(
                "Nodo Destino",
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = goalNodeId,
                onValueChange = { goalNodeId = it },
                label = { Text("Ej: 100, 200, 300...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Selector de perfil de seguridad
            Text(
                "Perfil de Seguridad",
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "fastest" to "⚡ Rápido",
                    "balanced" to "⚖️ Balanceado",
                    "safest" to "🛡️ Seguro"
                ).forEach { (profile, label) ->
                    FilterChip(
                        selected = selectedProfile == profile,
                        onClick = { selectedProfile = profile },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón calcular
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = {
                    val start = startNodeId.toIntOrNull()
                    val goal = goalNodeId.toIntOrNull()
                    if (start != null && goal != null) {
                        viewModel.calculateRoute(start, goal, selectedProfile)
                        onRouteCalculated()
                    }
                },
                enabled = !routeState.isLoading
            ) {
                if (routeState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Calcular Ruta")
                }
            }

            // Mostrar resultado
            routeState.route?.let { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "✅ Ruta Calculada",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text("Distancia: ${route.totalDistance}m")
                        Text("Costo: ${String.format("%.2f", route.totalCost)}")
                        Text("Nodos expandidos: ${route.expandedNodes}")
                        Text("Tiempo: ${route.calculationTime}ms")
                        Text(
                            "Camino: ${route.path.joinToString(" → ")}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Mostrar error
            routeState.error?.let { error ->
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
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
