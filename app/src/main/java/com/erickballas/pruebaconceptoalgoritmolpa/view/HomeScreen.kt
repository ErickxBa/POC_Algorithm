package com.erickballas.pruebaconceptoalgoritmolpa.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla principal/Home con opciones principales
 */
@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit = {},
    onNavigateToRoute: () -> Unit = {},
    onNavigateToIncident: () -> Unit = {},
    onNavigateToGraph: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚗 Alertify") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Bienvenida
            Text(
                "Bienvenido a Alertify",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Sistema inteligente de rutas seguras",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón Mapa
            MainMenuButton(
                icon = "📍",
                title = "Ver Mapa",
                description = "Visualiza la ciudad y rutas",
                onClick = onNavigateToMap
            )

            // Botón Calcular Ruta
            MainMenuButton(
                icon = "🛣️",
                title = "Calcular Ruta",
                description = "Encuentra la ruta más segura",
                onClick = onNavigateToRoute
            )

            // Botón Reportar Incidente
            MainMenuButton(
                icon = "⚠️",
                title = "Reportar Incidente",
                description = "Avisa sobre problemas en la vía",
                onClick = onNavigateToIncident
            )

            // Botón Gráfo
            MainMenuButton(
                icon = "📊",
                title = "Ver Gráfo",
                description = "Visualiza nodos y aristas",
                onClick = onNavigateToGraph
            )
        }
    }
}

@Composable
fun MainMenuButton(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "$icon $title",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
