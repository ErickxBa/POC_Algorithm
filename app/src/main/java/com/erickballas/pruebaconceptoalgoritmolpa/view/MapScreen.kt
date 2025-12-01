package com.erickballas.pruebaconceptoalgoritmolpa.view

import android.Manifest
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapIncident
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// Modos de interacción
enum class MapMode {
    EXPLORE, REPORT, NAVIGATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onIncidentClick: (String) -> Unit = {},
    // Ahora pasamos streetId también
    onNavigateToReport: (Double, Double, Int) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // ESTADO DEL MODO ACTUAL
    var currentMode by remember { mutableStateOf(MapMode.EXPLORE) }

    var isGraphInitialized by remember { mutableStateOf(false) }
    var hasCenteredMap by remember { mutableStateOf(false) }

    // Variables para el mapa
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapCenter by remember { mutableStateOf<GeoPoint?>(null) } // Coordenada central actual

    // Permisos y GPS (Igual que antes)
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions -> hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true }
    )

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
        if (!hasLocationPermission) permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val priority = Priority.PRIORITY_HIGH_ACCURACY
                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token).addOnSuccessListener { location ->
                    location?.let {
                        viewModel.setUserLocation(it.latitude, it.longitude)
                        if (!isGraphInitialized) {
                            viewModel.initializeGraphAtLocation(it.latitude, it.longitude)
                            isGraphInitialized = true
                        }
                    }
                }
            } catch (e: SecurityException) {}
        }
    }

    var showIncidentDialog by remember { mutableStateOf(false) }
    var selectedIncidentId by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {

        // --- 1. MAPA DE FONDO ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(10.3932, -75.4830))

                    // Listener para rastrear el centro del mapa cuando se mueve
                    addMapListener(object : org.osmdroid.events.MapListener {
                        override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                            // CORRECCIÓN: Accedemos al mapa usando 'this@apply'
                            val currentCenter = this@apply.mapCenter
                            // Actualizamos la variable de estado creando un nuevo GeoPoint limpio
                            mapCenter = GeoPoint(currentCenter.latitude, currentCenter.longitude)
                            return true
                        }
                        override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean = true
                    })

                    mapViewRef = this
                }
            },
            update = { view ->
                view.overlays.clear()

                // Actualizar referencia del centro siempre
                mapCenter = view.mapCenter as GeoPoint

                // Dibujar Ruta
                if (mapState.route.isNotEmpty()) {
                    val line = Polyline().apply {
                        setPoints(mapState.route.map { GeoPoint(it.latitude, it.longitude) })
                        outlinePaint.color = android.graphics.Color.BLUE
                        outlinePaint.strokeWidth = 15f // Más gruesa
                    }
                    view.overlays.add(line)
                }

                // Dibujar Incidentes (ROJOS y PELIGROSOS)
                mapState.incidents.forEach { incident ->
                    val marker = Marker(view).apply {
                        position = GeoPoint(incident.location.latitude, incident.location.longitude)
                        title = incident.type
                        snippet = "Riesgo: ${incident.severity}/10"
                        // Icono personalizado si quieres
                        // icon = ContextCompat.getDrawable(context, R.drawable.ic_warning)
                        setOnMarkerClickListener { _, _ ->
                            selectedIncidentId = incident.id
                            showIncidentDialog = true; true
                        }
                    }
                    view.overlays.add(marker)
                }

                // Usuario
                mapState.userLocation?.let { loc ->
                    val userPoint = GeoPoint(loc.latitude, loc.longitude)
                    view.overlays.add(Marker(view).apply {
                        position = userPoint
                        title = "Yo"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })
                    if (!hasCenteredMap) {
                        view.controller.animateTo(userPoint)
                        view.controller.setZoom(18.0)
                        hasCenteredMap = true
                    }
                }
                view.invalidate()
            }
        )

        // --- 2. PIN CENTRAL (ESTILO UBER) ---
        // Solo visible si estamos Reportando o Navegando
        if (currentMode != MapMode.EXPLORE) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Pin Central",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .offset(y = (-24).dp), // Subir para que la punta toque el centro
                tint = if (currentMode == MapMode.REPORT) Color.Red else Color.Blue
            )
        }

        // --- 3. BARRA SUPERIOR CON MODOS ---
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    // Modo Explorar
                    IconButton(onClick = { currentMode = MapMode.EXPLORE }) {
                        Icon(Icons.Default.Navigation, "Explorar", tint = if(currentMode == MapMode.EXPLORE) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                    // Modo Reportar
                    IconButton(onClick = { currentMode = MapMode.REPORT }) {
                        Icon(Icons.Default.AddAlert, "Reportar", tint = if(currentMode == MapMode.REPORT) Color.Red else Color.Gray)
                    }
                    // Modo Navegar
                    IconButton(onClick = { currentMode = MapMode.NAVIGATE }) {
                        Icon(Icons.Default.Directions, "Ir A...", tint = if(currentMode == MapMode.NAVIGATE) Color.Blue else Color.Gray)
                    }
                }
            }

            // Instrucciones según modo
            if (currentMode != MapMode.EXPLORE) {
                Surface(
                    modifier = Modifier.padding(top = 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = if (currentMode == MapMode.REPORT) "Mueve el mapa al lugar del incidente" else "Elige tu destino",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // --- 4. BOTÓN DE ACCIÓN PRINCIPAL (ABAJO) ---
        // Cambia según el modo
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (currentMode) {
                MapMode.EXPLORE -> {
                    // Botones normales (Limpiar, Centrar)
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { viewModel.clearRoute() }) { Text("Limpiar") }
                        Button(
                            onClick = {
                                mapState.userLocation?.let { loc ->
                                    viewModel.initializeGraphAtLocation(loc.latitude, loc.longitude)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) { Text("Reset Red") }
                        FloatingActionButton(
                            onClick = {
                                mapState.userLocation?.let { loc ->
                                    mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                                }
                            }
                        ) { Icon(Icons.Default.LocationOn, "Centrar") }
                    }
                }

                MapMode.REPORT -> {
                    Button(
                        onClick = {
                            val center = mapViewRef?.mapCenter // Obtenemos donde apunta el mapa
                            if (center != null) {
                                // Calculamos la calle más cercana
                                val nearestStreetId = viewModel.getNearestStreetId(center.latitude, center.longitude)
                                onNavigateToReport(center.latitude, center.longitude, nearestStreetId)
                                currentMode = MapMode.EXPLORE // Volver a normal
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("CONFIRMAR INCIDENTE AQUÍ")
                    }
                }

                MapMode.NAVIGATE -> {
                    Button(
                        onClick = {
                            val center = mapViewRef?.mapCenter
                            if (center != null) {
                                viewModel.calculateRouteToDestination(center.latitude, center.longitude)
                                currentMode = MapMode.EXPLORE
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) {
                        Text("TRAZAR RUTA SEGURA AQUÍ")
                    }
                }
            }
        }
    }

    if (showIncidentDialog) {
        val incident = mapState.incidents.find { it.id == selectedIncidentId }
        incident?.let { IncidentDetailDialog(incident = it, onDismiss = { showIncidentDialog = false }) }
    }
}

// ... CustomSearchBar e IncidentDetailDialog siguen igual ...
// (Asegúrate de copiarlas del código anterior si no las tienes aquí)
@Composable
fun CustomSearchBar(modifier: Modifier = Modifier) { /* ... */ }

@Composable
fun IncidentDetailDialog(incident: MapIncident, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle Incidente") },
        text = { Text("Tipo: ${incident.type}\nSeveridad: ${incident.severity}") },
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
    )
}