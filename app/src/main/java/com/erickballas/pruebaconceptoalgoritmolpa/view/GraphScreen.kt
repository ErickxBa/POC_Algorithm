package com.erickballas.pruebaconceptoalgoritmolpa.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.GraphViewModel

/**
 * Pantalla para visualizar el gráfo (nodos y aristas)
 */
@Composable
fun GraphScreen(
    viewModel: GraphViewModel,
    onBackClick: () -> Unit = {}
) {
    val graphState by viewModel.graphState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Gráfo de la Ciudad") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshGraphData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (graphState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Cargando gráfo...")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Estado del gráfo
                item {
                    graphState.status?.let { status ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Estado del Gráfo",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text("Estado: ${status.status}")
                                Text("Nodos: ${status.nodeCount}")
                                Text("Aristas: ${status.edgeCount}")
                                Text("Incidentes: ${status.incidentCount}")
                            }
                        }
                    }
                }

                // Nodos
                if (graphState.nodes.isNotEmpty()) {
                    item {
                        Text(
                            "📍 Nodos (${graphState.nodes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 16.dp)
                        )
                    }

                    items(graphState.nodes) { node ->
                        NodeCard(node = node)
                    }
                }

                // Aristas
                if (graphState.edges.isNotEmpty()) {
                    item {
                        Text(
                            "🔗 Aristas (${graphState.edges.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                        )
                    }

                    items(graphState.edges) { edge ->
                        EdgeCard(edge = edge)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Error
        graphState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    "❌ $error",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun NodeCard(node: com.erickballas.pruebaconceptoalgoritmolpa.model.Location) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Nodo ${node.id}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "Lat: ${String.format("%.4f", node.latitude)} | Lng: ${String.format("%.4f", node.longitude)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EdgeCard(edge: com.erickballas.pruebaconceptoalgoritmolpa.model.Street) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Nodo ${edge.from}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Text("→", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Nodo ${edge.to}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Distancia",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${edge.distance}m",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Riesgo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        String.format("%.2f", edge.risk),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Velocidad",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${edge.speedLimit} km/h",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// Importación de Refresh icon
import androidx.compose.material.icons.filled.Refresh
