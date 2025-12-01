package com.erickballas.pruebaconceptoalgoritmolpa.view

import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onIncidentClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()

    // Inicializar configuración de OSM (Importante para que cargue el mapa)
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    }

    var showIncidentDialog by remember { mutableStateOf(false) }
    var selectedIncidentId by remember { mutableStateOf("") }

    // Referencia al MapView para poder actualizarlo
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. EL MAPA (OSM)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK) // Estilo estándar de OSM
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)

                    // Centrar en Cartagena por defecto (según tus archivos)
                    controller.setCenter(GeoPoint(10.3932, -75.4830))

                    mapViewRef = this
                }
            },
            update = { view ->
                // Limpiar overlays anteriores para redibujar
                view.overlays.clear()

                // A. DIBUJAR RUTA (Si existe)
                if (mapState.route.isNotEmpty()) {
                    val line = Polyline().apply {
                        // Convertir GeoLocation a GeoPoint de OSM
                        val points = mapState.route.map { GeoPoint(it.latitude, it.longitude) }
                        setPoints(points)
                        outlinePaint.color = android.graphics.Color.BLUE
                        outlinePaint.strokeWidth = 10f
                    }
                    view.overlays.add(line)
                }

                // B. DIBUJAR INCIDENTES
                mapState.incidents.forEach { incident ->
                    val marker = Marker(view).apply {
                        position = GeoPoint(incident.location.latitude, incident.location.longitude)
                        title = incident.type.uppercase()
                        snippet = "Severidad: ${incident.severity}/10"

                        // Configurar icono o color según severidad (Lógica simplificada)
                        // En OSM los iconos requieren drawables, aquí usamos el default por simplicidad
                        // Puedes usar: icon = ContextCompat.getDrawable(context, R.drawable.tu_icono)

                        setOnMarkerClickListener { _, _ ->
                            selectedIncidentId = incident.id
                            showIncidentDialog = true
                            onIncidentClick(incident.id)
                            true
                        }
                    }
                    view.overlays.add(marker)
                }

                // C. UBICACIÓN DEL USUARIO
                mapState.userLocation?.let { location ->
                    val userMarker = Marker(view).apply {
                        position = GeoPoint(location.latitude, location.longitude)
                        title = "Tu ubicación"
                        // icon = ... (poner un icono diferente para el usuario)
                    }
                    view.overlays.add(userMarker)
                }

                // Forzar repintado
                view.invalidate()
            }
        )

        // 2. PANELES UI (Igual que en tu código original)
        // Panel superior con búsqueda
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            SearchBar(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            if (mapState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
            }
        }

        // Panel inferior con controles
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), MaterialTheme.shapes.medium)
                .padding(12.dp)
        ) {
            if (mapState.incidents.isNotEmpty()) {
                Text(
                    text = "📍 ${mapState.incidents.size} incidentes cercanos",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.clearRoute() }
                ) { Text("Limpiar") }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        mapState.userLocation?.let { loc ->
                            mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                        }
                    }
                ) { Text("Mi Ubicación") }
            }
        }
    }

    // Diálogo de detalles (Igual que el original)
    if (showIncidentDialog) {
        val incident = mapState.incidents.find { it.id == selectedIncidentId }
        incident?.let {
            IncidentDetailDialog(incident = it, onDismiss = { showIncidentDialog = false })
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
                Button(
                    onClick = { searchText = "" },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.textButtonColors()
                ) {
                    Text("✕")
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
