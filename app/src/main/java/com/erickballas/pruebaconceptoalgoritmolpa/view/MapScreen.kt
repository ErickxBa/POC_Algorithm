package com.erickballas.pruebaconceptoalgoritmolpa.view

import android.Manifest
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

enum class MapMode { EXPLORE, REPORT, NAVIGATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onSearchClick: () -> Unit = {},
    onNavigateToReport: (Double, Double, Int) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    // IMPORTANTE: Recogemos el estado. Cada cambio aquí forzará redibujado
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentMode by remember { mutableStateOf(MapMode.EXPLORE) }
    var isGraphInitialized by remember { mutableStateOf(false) }
    var hasCenteredMap by remember { mutableStateOf(false) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var mapCenter by remember { mutableStateOf<GeoPoint?>(null) }

    // Permisos
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

    // Auto-refresco al volver a la pantalla
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mapState.userLocation?.let { viewModel.setUserLocation(it.latitude, it.longitude) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // GPS Inicial
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val priority = Priority.PRIORITY_HIGH_ACCURACY
                val token = CancellationTokenSource().token
                fusedLocationClient.getCurrentLocation(priority, token).addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.setUserLocation(location.latitude, location.longitude)
                        if (!isGraphInitialized) {
                            viewModel.initializeGraphAtLocation(location.latitude, location.longitude)
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

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(10.3932, -75.4830))

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
            // EL BLOQUE UPDATE ES CLAVE: Se ejecuta cada vez que mapState cambia
            update = { view ->
                Log.d("MapScreen", "Redibujando mapa. Incidentes: ${mapState.incidents.size}, Ruta: ${mapState.route.size}")

                view.overlays.clear()
                mapCenter = view.mapCenter as GeoPoint

                // --- 1. DIBUJAR INCIDENTES (CÍRCULOS ROJOS GRANDES) ---
                mapState.incidents.forEach { incident ->
                    val geoPoint = GeoPoint(incident.location.latitude, incident.location.longitude)

                    // Círculo de "zona peligrosa" (Estilo Figma)
                    val dangerZone = Polygon().apply {
                        points = Polygon.pointsAsCircle(geoPoint, 150.0) // 150 metros de radio
                        fillPaint.color = android.graphics.Color.parseColor("#4DFF0000") // Rojo semitransparente
                        outlinePaint.color = android.graphics.Color.RED
                        outlinePaint.strokeWidth = 2f
                    }
                    view.overlays.add(dangerZone)

                    // Marcador Icono
                    val marker = Marker(view).apply {
                        position = geoPoint
                        title = incident.type.uppercase()
                        snippet = "Riesgo: ${incident.severity}/10"
                        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_delete) // Icono temporal alerta
                        setOnMarkerClickListener { _, _ ->
                            selectedIncidentId = incident.id
                            showIncidentDialog = true; true
                        }
                    }
                    view.overlays.add(marker)
                }

                // --- 2. DIBUJAR RUTA ---
                if (mapState.route.isNotEmpty()) {
                    val line = Polyline().apply {
                        setPoints(mapState.route.map { GeoPoint(it.latitude, it.longitude) })
                        outlinePaint.color = android.graphics.Color.BLUE
                        outlinePaint.strokeWidth = 15f
                    }
                    view.overlays.add(line)
                }

                // --- 3. USUARIO ---
                mapState.userLocation?.let { loc ->
                    val userPoint = GeoPoint(loc.latitude, loc.longitude)
                    val userMarker = Marker(view).apply {
                        position = userPoint; title = "Yo"; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    view.overlays.add(userMarker)

                    if (!hasCenteredMap) {
                        view.controller.animateTo(userPoint)
                        view.controller.setZoom(17.0)
                        hasCenteredMap = true
                    }
                }

                view.invalidate() // Forzar repintado
            }
        )

        // ... (Resto de UI: Pin Central, Barra Búsqueda, Botones) ...
        // (Mantén el código de UI de botones tal cual estaba en la versión anterior, es correcto)

        // PIN CENTRAL
        if (currentMode == MapMode.REPORT) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Pin",
                modifier = Modifier.size(48.dp).align(Alignment.Center).offset(y = (-24).dp),
                tint = Color.Red
            )
        }

        // BARRA SUPERIOR
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(56.dp).clickable { onSearchClick() },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(Icons.Default.Search, null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("¿A dónde quieres ir?", color = Color.Gray)
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
                    FloatingActionButton(onClick = { mapState.userLocation?.let { mapViewRef?.controller?.animateTo(GeoPoint(it.latitude, it.longitude)) } }) {
                        Text("📍", modifier = Modifier.padding(16.dp))
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("CONFIRMAR INCIDENTE AQUÍ") }

                IconButton(onClick = { currentMode = MapMode.EXPLORE }, modifier = Modifier.align(Alignment.TopEnd).offset(y = (-60).dp).background(Color.White, shape = MaterialTheme.shapes.small)) {
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