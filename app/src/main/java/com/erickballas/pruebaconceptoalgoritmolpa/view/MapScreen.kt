package com.erickballas.pruebaconceptoalgoritmolpa.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

enum class MapMode { EXPLORE, REPORT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onSearchClick: () -> Unit = {},
    onNavigateToReport: (Double, Double, Int) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()

    // Cliente de Ubicación de Google (GPS)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var currentMode by remember { mutableStateOf(MapMode.EXPLORE) }
    var isGraphInitialized by remember { mutableStateOf(false) }

    // Control para saber si ya centramos el mapa en el usuario al menos una vez
    var hasCenteredMap by remember { mutableStateOf(false) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapCenter by remember { mutableStateOf<GeoPoint?>(null) }

    // --- 1. GESTIÓN DE PERMISOS ---
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        }
    )

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName

        // Si no tiene permisos, pedirlos
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // --- 2. OBTENER UBICACIÓN REAL (GPS) ---
    // Esta función se llama:
    // a) Cuando se otorgan permisos
    // b) Cuando presionamos el botón de "Centrar"
    fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                // Usamos PRIORITY_HIGH_ACCURACY para forzar el GPS real
                val priority = Priority.PRIORITY_HIGH_ACCURACY
                val tokenSource = CancellationTokenSource()

                fusedLocationClient.getCurrentLocation(priority, tokenSource.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            Log.d("GPS", "Ubicación encontrada: ${location.latitude}, ${location.longitude}")

                            // 1. Guardar en ViewModel
                            viewModel.setUserLocation(location.latitude, location.longitude)

                            // 2. Mover la cámara del mapa
                            mapViewRef?.controller?.animateTo(GeoPoint(location.latitude, location.longitude))
                            mapViewRef?.controller?.setZoom(18.0)
                            hasCenteredMap = true

                            // 3. Crear red si es la primera vez
                            if (!isGraphInitialized) {
                                viewModel.initializeGraphAtLocation(location.latitude, location.longitude)
                                isGraphInitialized = true
                            }
                        } else {
                            Toast.makeText(context, "Activando GPS...", Toast.LENGTH_SHORT).show()
                            // Si falla getCurrentLocation, intentamos con getLastLocation
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                lastLoc?.let {
                                    viewModel.setUserLocation(it.latitude, it.longitude)
                                    mapViewRef?.controller?.animateTo(GeoPoint(it.latitude, it.longitude))
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("GPS", "Error obteniendo ubicación: ${e.message}")
                    }
            } catch (e: SecurityException) {
                Log.e("GPS", "Permiso denegado: ${e.message}")
            }
        }
    }

    // Intentar obtener ubicación al iniciar si ya hay permiso
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            getCurrentLocation()
        }
    }

    // Recargar datos al volver a la pantalla
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasLocationPermission) {
                getCurrentLocation() // Refrescar ubicación al volver
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showIncidentDialog by remember { mutableStateOf(false) }
    var selectedIncidentId by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    // COORDENADA POR DEFECTO (Solo se usa si el GPS falla o tarda en cargar)
                    // Quito, Ecuador (puedes cambiar esto si quieres que empiece en otro lado mientras carga)
                    val startPoint = GeoPoint(-0.1807, -78.4678)
                    controller.setZoom(15.0)
                    controller.setCenter(startPoint)

                    addMapListener(object : org.osmdroid.events.MapListener {
                        override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                            val current = this@apply.mapCenter
                            mapCenter = GeoPoint(current.latitude, current.longitude)
                            return true
                        }
                        override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean = true
                    })
                    mapViewRef = this
                }
            },
            update = { view ->
                view.overlays.clear()
                mapCenter = view.mapCenter as GeoPoint

                // Dibujar Incidentes
                mapState.incidents.forEach { incident ->
                    val geoPoint = GeoPoint(incident.location.latitude, incident.location.longitude)

                    // Zona de peligro (Círculo rojo)
                    val dangerZone = Polygon().apply {
                        points = Polygon.pointsAsCircle(geoPoint, 100.0)
                        fillPaint.color = android.graphics.Color.parseColor("#4DFF0000")
                        outlinePaint.color = android.graphics.Color.RED
                        outlinePaint.strokeWidth = 2f
                    }
                    view.overlays.add(dangerZone)

                    // Marcador
                    val marker = Marker(view).apply {
                        position = geoPoint
                        title = incident.type
                        snippet = "Riesgo: ${incident.severity}/10"
                        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_alert)
                        setOnMarkerClickListener { _, _ ->
                            selectedIncidentId = incident.id
                            showIncidentDialog = true; true
                        }
                    }
                    view.overlays.add(marker)
                }

                // Dibujar Ruta
                if (mapState.route.isNotEmpty()) {
                    val line = Polyline().apply {
                        setPoints(mapState.route.map { GeoPoint(it.latitude, it.longitude) })
                        outlinePaint.color = android.graphics.Color.BLUE
                        outlinePaint.strokeWidth = 15f
                    }
                    view.overlays.add(line)
                }

                // Dibujar Usuario (Tu Ubicación Real)
                mapState.userLocation?.let { loc ->
                    val userPoint = GeoPoint(loc.latitude, loc.longitude)
                    val userMarker = Marker(view).apply {
                        position = userPoint
                        title = "Mi Ubicación"
                        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    view.overlays.add(userMarker)

                    // Auto-Centrado inicial (Solo la primera vez que detecta GPS)
                    if (!hasCenteredMap) {
                        view.controller.animateTo(userPoint)
                        view.controller.setZoom(18.0)
                        hasCenteredMap = true
                    }
                }
                view.invalidate()
            }
        )

        // PIN CENTRAL (Solo en modo reporte)
        if (currentMode == MapMode.REPORT) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Pin",
                modifier = Modifier.size(48.dp).align(Alignment.Center).offset(y = (-24).dp),
                tint = ComposeColor.Red
            )
        }

        // BARRA DE BÚSQUEDA
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(56.dp).clickable { onSearchClick() },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(Icons.Default.Search, null, tint = ComposeColor.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("¿A dónde quieres ir?", color = ComposeColor.Gray)
                }
            }
            if (mapState.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }

        // CONTROLES INFERIORES
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            if (currentMode == MapMode.EXPLORE) {
                Row(modifier = Modifier.align(Alignment.BottomEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(onClick = { currentMode = MapMode.REPORT }, containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text("⚠️", modifier = Modifier.padding(16.dp))
                    }

                    // BOTÓN CENTRAR (Fuerza la actualización del GPS)
                    FloatingActionButton(onClick = { getCurrentLocation() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Centrar")
                    }
                }
                Row(modifier = Modifier.align(Alignment.BottomStart)) {
                    Button(
                        onClick = { mapState.userLocation?.let { viewModel.initializeGraphAtLocation(it.latitude, it.longitude) } },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Crear Red") }
                    Button(onClick = { viewModel.clearRoute() }) { Text("X Ruta") }
                }
            } else {
                Button(
                    onClick = {
                        val center = mapViewRef?.mapCenter
                        if (center != null) {
                            val nearestStreetId = viewModel.getNearestStreetId(center.latitude, center.longitude)
                            onNavigateToReport(center.latitude, center.longitude, nearestStreetId)
                            currentMode = MapMode.EXPLORE
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ComposeColor.Red)
                ) { Text("CONFIRMAR AQUÍ") }

                IconButton(onClick = { currentMode = MapMode.EXPLORE }, modifier = Modifier.align(Alignment.TopEnd).offset(y = (-60).dp).background(ComposeColor.White, shape = MaterialTheme.shapes.small)) {
                    Icon(Icons.Default.Clear, null)
                }
            }
        }
    }

    if (showIncidentDialog) {
        val incident = mapState.incidents.find { it.id == selectedIncidentId }
        incident?.let { IncidentDetailDialog(incident = it, onDismiss = { showIncidentDialog = false }) }
    }
}

@Composable
fun IncidentDetailDialog(incident: MapIncident, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle Incidente") },
        text = { Text("Tipo: ${incident.type}\nSeveridad: ${incident.severity}") },
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
    )
}