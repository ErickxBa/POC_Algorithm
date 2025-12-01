package com.erickballas.pruebaconceptoalgoritmolpa.view

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.GraphViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    viewModel: GraphViewModel,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()
    val edges by viewModel.edges.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
        viewModel.loadGraphData() // Cargar datos al entrar
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visualizador de Grafo") },
                navigationIcon = { Button(onClick = onBackClick) { Text("Atrás") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        controller.setCenter(GeoPoint(10.3950, -75.4900))
                    }
                },
                update = { view ->
                    view.overlays.clear()

                    // Dibujar Calles
                    edges.forEach { edge ->
                        val start = nodes.find { it.nodeId == edge.fromNodeId }
                        val end = nodes.find { it.nodeId == edge.toNodeId }
                        if (start != null && end != null) {
                            val line = Polyline().apply {
                                setPoints(listOf(GeoPoint(start.latitude, start.longitude), GeoPoint(end.latitude, end.longitude)))
                                outlinePaint.color = android.graphics.Color.BLACK
                                outlinePaint.strokeWidth = 5f
                            }
                            view.overlays.add(line)
                        }
                    }

                    // Dibujar Nodos
                    nodes.forEach { node ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(node.latitude, node.longitude)
                            title = "Nodo ${node.nodeId}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        view.overlays.add(marker)
                    }
                    view.invalidate()
                }
            )
        }
    }
}