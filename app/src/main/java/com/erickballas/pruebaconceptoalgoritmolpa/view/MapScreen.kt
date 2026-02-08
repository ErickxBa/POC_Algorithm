package com.erickballas.pruebaconceptoalgoritmolpa.view

import android.Manifest
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onSearchClick: () -> Unit = {},
    onNavigateToReport: (Double, Double, Int) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var currentMode by remember { mutableStateOf(MapMode.EXPLORE) }
    var hasCenteredMap by remember { mutableStateOf(false) }
    var isGraphInitialized by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // --- Permisos ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    // --- Lógica Automática ---
    fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    location?.let {
                        viewModel.setUserLocation(it.latitude, it.longitude)

                        // Centrar mapa si es la primera vez
                        if (!hasCenteredMap) {
                            mapViewRef?.controller?.animateTo(GeoPoint(it.latitude, it.longitude))
                            mapViewRef?.controller?.setZoom(18.0)
                            hasCenteredMap = true
                        }

                        // AUTO-INICIALIZAR RED (Sin botón)
                        if (!isGraphInitialized) {
                            viewModel.initializeGraphAtLocation(it.latitude, it.longitude)
                            isGraphInitialized = true
                        }
                    }
                }
        }
    }

    // Intentar obtener ubicación al iniciar
    LaunchedEffect(Unit) { getCurrentLocation() }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- MAPA ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(-0.1807, -78.4678)) // Fallback Quito
                    mapViewRef = this
                }
            },
            update = { view ->
                view.overlays.clear()

                // 1. RUTA (Línea Azul) - Se dibuja si el VM tiene datos
                if (mapState.route.isNotEmpty()) {
                    val line = Polyline().apply {
                        setPoints(mapState.route.map { GeoPoint(it.latitude, it.longitude) })
                        outlinePaint.color = android.graphics.Color.BLUE
                        outlinePaint.strokeWidth = 15f
                        isGeodesic = true
                    }
                    view.overlays.add(line)
                }

                // 2. Incidentes
                mapState.incidents.forEach { incident ->
                    val p = GeoPoint(incident.location.latitude, incident.location.longitude)
                    view.overlays.add(Polygon().apply {
                        points = Polygon.pointsAsCircle(p, 80.0)
                        fillPaint.color = android.graphics.Color.parseColor("#33FF0000")
                        outlinePaint.color = android.graphics.Color.RED
                        outlinePaint.strokeWidth = 1f
                    })
                    view.overlays.add(Marker(view).apply {
                        position = p; title = incident.type
                        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_dialog_alert)
                    })
                }

                // 3. Usuario
                mapState.userLocation?.let {
                    val p = GeoPoint(it.latitude, it.longitude)
                    view.overlays.add(Marker(view).apply {
                        position = p; title = "Yo"
                        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    })
                }
                view.invalidate()
            }
        )

        // --- BARRA SUPERIOR (Buscador) ---
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth().height(56.dp)
                .clickable { onSearchClick() },
            shape = MaterialTheme.shapes.medium, shadowElevation = 4.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Icon(Icons.Default.Search, null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                // Mensaje dinámico según estado
                Text(
                    text = if (mapState.route.isNotEmpty()) "Ruta Trazada (Toca para cambiar)" else "¿A dónde quieres ir?",
                    color = Color.Gray
                )
            }
        }

        // --- BOTONES FLOTANTES ---
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            if (currentMode == MapMode.EXPLORE) {
                Row(modifier = Modifier.align(Alignment.BottomEnd)) {
                    FloatingActionButton(onClick = { currentMode = MapMode.REPORT }, containerColor = MaterialTheme.colorScheme.errorContainer) { Text("⚠️") }
                    Spacer(modifier = Modifier.width(16.dp))
                    FloatingActionButton(onClick = { getCurrentLocation() }) { Icon(Icons.Default.MyLocation, "Centrar") }
                }
                // (Botón "Crear Red" eliminado, ahora es automático)
                // (Botón "Borrar Ruta" eliminado para limpieza, se borra al buscar nueva)
            } else {
                // MODO REPORTE
                Button(
                    onClick = {
                        mapViewRef?.mapCenter?.let { c ->
                            onNavigateToReport(c.latitude, c.longitude, viewModel.getNearestStreetId(c.latitude, c.longitude))
                            currentMode = MapMode.EXPLORE
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("CONFIRMAR UBICACIÓN") }

                IconButton(onClick = { currentMode = MapMode.EXPLORE }, modifier = Modifier.align(Alignment.TopEnd).offset(y = (-70).dp).background(Color.White)) { Icon(Icons.Default.Clear, null) }
                Icon(Icons.Default.LocationOn, null, Modifier.size(48.dp).align(Alignment.Center).offset(y = (-24).dp), tint = Color.Red)
            }
        }

        if (mapState.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
    }
}