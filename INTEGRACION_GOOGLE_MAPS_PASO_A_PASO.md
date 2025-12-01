# 📍 Integración Google Maps API - Guía Paso a Paso

## 📋 Índice
1. [Obtener API Key](#1-obtener-api-key)
2. [Configurar Android](#2-configurar-android)
3. [Crear Pantalla de Mapa](#3-crear-pantalla-de-mapa)
4. [Dibujar Rutas](#4-dibujar-rutas)
5. [Mostrar Incidentes](#5-mostrar-incidentes)
6. [Ubicación en Tiempo Real](#6-ubicación-en-tiempo-real)
7. [Pruebas](#7-pruebas)

---

## 1️⃣ Obtener API Key

### Paso 1.1: Crear Proyecto en Google Cloud

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Haz clic en el **selector de proyecto** (arriba a la izquierda)
3. Selecciona **"NUEVO PROYECTO"**
4. Dale nombre: `AlertifyMaps`
5. Haz clic en **"CREAR"**

### Paso 1.2: Activar APIs Necesarias

1. Ve a **APIs & Services** → **Library**
2. Busca y activa estas APIs:
   - ✅ **Maps SDK for Android**
   - ✅ **Directions API** (para rutas)
   - ✅ **Places API** (opcional, para buscar lugares)
   - ✅ **Geolocation API** (para ubicación)

### Paso 1.3: Crear Credenciales

1. Ve a **APIs & Services** → **Credentials**
2. Haz clic en **"+ CREATE CREDENTIALS"** → **"API Key"**
3. Se creará una clave API (ejemplo: `AIzaSyD_example_key_1234567890`)
4. **Guarda esta clave** en un lugar seguro

### Paso 1.4: Configurar Restricciones

1. En la clave API creada, haz clic en ella
2. Ve a **Application restrictions**
3. Selecciona **"Android apps"**
4. Haz clic en **"Add an Android app"**
5. Necesitas el **SHA-1 fingerprint** de tu app

---

## 2️⃣ Configurar Android

### Paso 2.1: Obtener SHA-1 Fingerprint

Abre terminal en la carpeta del proyecto Android:

```bash
# Windows
./gradlew signingReport

# Mac/Linux
./gradlew signingReport
```

Busca la línea que dice:
```
SHA-1: AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12
```

Copia ese valor.

### Paso 2.2: Agregar SHA-1 a Google Cloud

1. En la credencial API Key de Google Cloud
2. En **Android apps**, pega el SHA-1
3. Agrega el **Package name**: `com.erickballas.pruebaconceptoalgoritmolpa`
4. Haz clic en **"SAVE"**

### Paso 2.3: Agregar Dependencias

En `app/build.gradle.kts`:

```kotlin
dependencies {
    // Google Play Services (Maps)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Kotlin Extensions
    implementation("com.google.maps.android:maps-ktx:5.0.1")
    implementation("com.google.maps.android:maps-compose:4.3.6")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
```

Haz clic en **"Sync Now"**

### Paso 2.4: Agregar API Key al Proyecto

En `app/src/main/AndroidManifest.xml`, agrega dentro del tag `<application>`:

```xml
<application>
    ...
    
    <!-- Google Maps API Key -->
    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="AIzaSyD_your_actual_api_key_here" />
    
    ...
</application>
```

### Paso 2.5: Agregar Permisos

En `app/src/main/AndroidManifest.xml`, agrega fuera de `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

---

## 3️⃣ Crear Pantalla de Mapa

### Paso 3.1: Crear ViewModel para Mapa

**Archivo:** `src/main/kotlin/.../viewmodel/MapViewModel.kt`

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class MapState(
    val isLoading: Boolean = false,
    val userLocation: LatLng? = null,
    val route: List<LatLng> = emptyList(),
    val incidents: List<MapIncident> = emptyList(),
    val error: String? = null,
    val zoom: Float = 15f
)

data class MapIncident(
    val id: String,
    val location: LatLng,
    val type: String,
    val severity: Int
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: GraphRepository
) : ViewModel() {

    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState

    fun setUserLocation(lat: Double, lng: Double) {
        _mapState.value = _mapState.value.copy(
            userLocation = LatLng(lat, lng)
        )
    }

    fun displayRoute(nodeIds: List<Int>) {
        viewModelScope.launch {
            try {
                val nodes = repository.getGraphNodes()
                val route = nodeIds.mapNotNull { id ->
                    nodes.find { it.nodeId == id }?.let { 
                        LatLng(it.latitude, it.longitude)
                    }
                }
                _mapState.value = _mapState.value.copy(route = route)
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = e.message)
            }
        }
    }

    fun loadNearbyIncidents(lat: Double, lng: Double, radiusMeters: Int = 5000) {
        viewModelScope.launch {
            try {
                val incidents = repository.getNearbyIncidents(lat, lng, radiusMeters)
                val mapIncidents = incidents.map { incident ->
                    MapIncident(
                        id = incident.reportId,
                        location = LatLng(incident.latitude, incident.longitude),
                        type = incident.incidentType,
                        severity = incident.severity
                    )
                }
                _mapState.value = _mapState.value.copy(incidents = mapIncidents)
            } catch (e: Exception) {
                _mapState.value = _mapState.value.copy(error = e.message)
            }
        }
    }

    fun clearRoute() {
        _mapState.value = _mapState.value.copy(route = emptyList())
    }
}
```

### Paso 3.2: Crear Composable del Mapa

**Archivo:** `src/main/kotlin/.../ui/screens/MapScreen.kt`

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.compose.*
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapViewModel

@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    onIncidentClick: (String) -> Unit = {}
) {
    val mapState by mapViewModel.mapState.collectAsStateWithLifecycle()
    
    // Ubicación inicial (Cartagena, Colombia)
    val initialLocation = LatLng(10.3932, -75.4830)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
            initialLocation,
            mapState.zoom
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Mapa
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapType = MapType.NORMAL
            )
        ) {
            // Marcador de ubicación del usuario
            mapState.userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Tu ubicación",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_BLUE
                    )
                )
            }

            // Dibujar ruta
            if (mapState.route.isNotEmpty()) {
                Polyline(
                    points = mapState.route,
                    color = android.graphics.Color.BLUE,
                    width = 8f,
                    geodesic = true
                )
                
                // Marcadores de inicio y fin
                mapState.route.firstOrNull()?.let { start ->
                    Marker(
                        state = MarkerState(position = start),
                        title = "Inicio",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }
                
                mapState.route.lastOrNull()?.let { end ->
                    Marker(
                        state = MarkerState(position = end),
                        title = "Destino",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }
            }

            // Mostrar incidentes
            mapState.incidents.forEach { incident ->
                Marker(
                    state = MarkerState(position = incident.location),
                    title = incident.type,
                    snippet = "Severidad: ${incident.severity}/10",
                    onClick = {
                        onIncidentClick(incident.id)
                        true
                    },
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when (incident.severity) {
                            in 1..3 -> BitmapDescriptorFactory.HUE_YELLOW
                            in 4..6 -> BitmapDescriptorFactory.HUE_ORANGE
                            else -> BitmapDescriptorFactory.HUE_RED
                        }
                    )
                )
            }
        }

        // Panel superior
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Buscador (opcional)
            SearchBar(
                modifier = Modifier.fillMaxWidth(),
                onSearch = { query ->
                    // Buscar lugares aquí
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar estado
            if (mapState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            mapState.error?.let { error ->
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.errorContainer)
                )
            }
        }

        // Panel inferior con controles
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (mapState.incidents.isNotEmpty()) {
                Text(
                    text = "Incidentes cercanos: ${mapState.incidents.size}",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = { mapViewModel.clearRoute() }
                ) {
                    Text("Limpiar")
                }

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = { 
                        // Centrar en ubicación del usuario
                        mapState.userLocation?.let { location ->
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(location, 15f)
                            )
                        }
                    }
                ) {
                    Text("Mi Ubicación")
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    
    OutlinedTextField(
        value = searchText,
        onValueChange = { searchText = it },
        modifier = modifier,
        placeholder = { Text("Buscar ubicación...") },
        singleLine = true,
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { searchText = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                }
            }
        }
    )
}
```

---

## 4️⃣ Dibujar Rutas

### Paso 4.1: Crear Servicio de Rutas

**Archivo:** `src/main/kotlin/.../service/RouteService.kt`

```kotlin
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow

class RouteService(
    private val repository: GraphRepository,
    private val mapViewModel: MapViewModel
) {
    
    /**
     * Calcula una ruta entre dos nodos y la muestra en el mapa
     */
    suspend fun calculateAndDisplayRoute(
        startNodeId: Int,
        goalNodeId: Int,
        safetyProfile: String = "balanced"
    ) {
        try {
            repository.calculateRoute(startNodeId, goalNodeId, safetyProfile)
                .collect { route ->
                    // La ruta contiene: path (lista de nodeIds)
                    mapViewModel.displayRoute(route.path)
                }
        } catch (e: Exception) {
            println("Error calculando ruta: ${e.message}")
        }
    }

    /**
     * Dibuja una ruta alternativa con color diferente
     */
    fun drawAlternativeRoute(nodeIds: List<Int>, color: Int) {
        mapViewModel.displayRoute(nodeIds)
    }

    /**
     * Limpia la ruta del mapa
     */
    fun clearRoute() {
        mapViewModel.clearRoute()
    }
}
```

### Paso 4.2: Usar en Pantalla

```kotlin
@Composable
fun RoutePlanningScreen(
    routeService: RouteService,
    mapViewModel: MapViewModel
) {
    var startNodeId by remember { mutableStateOf(100) }
    var goalNodeId by remember { mutableStateOf(500) }
    var safetyProfile by remember { mutableStateOf("balanced") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Selector de nodo inicial
        OutlinedTextField(
            value = startNodeId.toString(),
            onValueChange = { startNodeId = it.toIntOrNull() ?: 100 },
            label = { Text("Nodo inicial") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Selector de nodo destino
        OutlinedTextField(
            value = goalNodeId.toString(),
            onValueChange = { goalNodeId = it.toIntOrNull() ?: 500 },
            label = { Text("Nodo destino") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Selector de perfil de seguridad
        Text("Perfil de Seguridad:")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("fastest", "balanced", "safest").forEach { profile ->
                FilterChip(
                    selected = safetyProfile == profile,
                    onClick = { safetyProfile = profile },
                    label = { Text(profile) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para calcular ruta
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            onClick = {
                // Ejecutar en corrutina
                // routeService.calculateAndDisplayRoute(startNodeId, goalNodeId, safetyProfile)
            }
        ) {
            Text("Calcular Ruta")
        }
    }
}
```

---

## 5️⃣ Mostrar Incidentes

### Paso 5.1: Crear Marcadores de Incidentes

```kotlin
// En GoogleMap composable
mapState.incidents.forEach { incident ->
    Marker(
        state = MarkerState(position = incident.location),
        title = incident.type.uppercase(),
        snippet = "Severidad: ${incident.severity}/10",
        onClick = {
            // Mostrar diálogo con detalles
            showIncidentDetails = true
            selectedIncident = incident
            true
        },
        icon = BitmapDescriptorFactory.defaultMarker(
            when (incident.severity) {
                in 1..3 -> BitmapDescriptorFactory.HUE_YELLOW      // Bajo
                in 4..6 -> BitmapDescriptorFactory.HUE_ORANGE      // Medio
                else -> BitmapDescriptorFactory.HUE_RED            // Alto
            }
        ),
        infoWindow = {
            Text(
                "${incident.type}: ${incident.severity}/10",
                style = MaterialTheme.typography.labelMedium
            )
        }
    )
}
```

### Paso 5.2: Diálogo de Detalles de Incidente

```kotlin
if (showIncidentDetails && selectedIncident != null) {
    AlertDialog(
        onDismissRequest = { showIncidentDetails = false },
        title = { Text("Incidente Reportado") },
        text = {
            Column {
                Text("Tipo: ${selectedIncident.type}")
                Text("Severidad: ${selectedIncident.severity}/10")
                Text("Ubicación: ${selectedIncident.location.latitude}, ${selectedIncident.location.longitude}")
            }
        },
        confirmButton = {
            TextButton(onClick = { showIncidentDetails = false }) {
                Text("Cerrar")
            }
        }
    )
}
```

---

## 6️⃣ Ubicación en Tiempo Real

### Paso 6.1: Obtener Ubicación

**Archivo:** `src/main/kotlin/.../service/LocationService.kt`

```kotlin
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class LocationService(context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    suspend fun getCurrentLocation(): Location? {
        return try {
            val cancellationToken = CancellationTokenSource().token
            
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken
            ).await()
        } catch (e: Exception) {
            println("Error obteniendo ubicación: ${e.message}")
            null
        }
    }

    suspend fun startLocationUpdates() {
        val location = getCurrentLocation()
        if (location != null) {
            _location.value = location
        }
    }
}
```

### Paso 6.2: Pedir Permisos en Android

```kotlin
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun LocationPermissionScreen() {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido
        } else {
            // Permiso denegado
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
```

---

## 7️⃣ Pruebas

### Paso 7.1: Verificar Configuración

Checklist de validación:

- [ ] API Key creada en Google Cloud Console
- [ ] Maps SDK for Android habilitado
- [ ] SHA-1 fingerprint registrado en credenciales
- [ ] API Key agregada en AndroidManifest.xml
- [ ] Permisos de ubicación en AndroidManifest.xml
- [ ] Dependencias de Google Play Services agregadas
- [ ] Proyecto sincronizado (Sync Now)

### Paso 7.2: Probar en Emulador

```bash
# Desde Android Studio, abre el emulador
# Luego ejecuta:
./gradlew installDebug
```

### Paso 7.3: Simular Ubicación

En Android Studio:
1. Abre **Device File Explorer**
2. Navega a `/data/data/com.example.app/`
3. Busca archivos de configuración

O usa la terminal:
```bash
adb shell am startservice -a com.google.android.gms.FAKE_LOCATION
```

### Paso 7.4: Pruebas de API

Desde el teléfono/emulador:

```kotlin
// En un botón de test
Button(onClick = {
    viewModel.calculateRoute(100, 500, "balanced")
}) {
    Text("Calcular Ruta de Prueba")
}

Button(onClick = {
    viewModel.loadNearbyIncidents(10.3932, -75.4830)
}) {
    Text("Cargar Incidentes Cercanos")
}
```

---

## 🔍 Solución de Problemas

### Problema: "This API project is not authorized"
**Solución:** 
- Verifica que la API Key esté correcta en AndroidManifest.xml
- Confirma que Maps SDK for Android esté habilitado en Google Cloud

### Problema: El mapa no aparece
**Solución:**
- Verifica que el SHA-1 sea correcto (`./gradlew signingReport`)
- Recrea la credencial API Key
- Reinicia el emulador/teléfono

### Problema: "Permission denied"
**Solución:**
- Pide permisos en runtime:
```kotlin
if (ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_FINE_LOCATION
) != PackageManager.PERMISSION_GRANTED
) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        PERMISSION_REQUEST_CODE
    )
}
```

### Problema: Las rutas no se dibujan
**Solución:**
- Verifica que los nodeIds sean válidos
- Confirma que el Backend está corriendo
- Revisa logs con: `adb logcat`

---

## 📚 Referencias Útiles

- [Google Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk)
- [Maps Compose Library](https://developers.google.com/maps/documentation/android-sdk/maps-compose)
- [Directions API](https://developers.google.com/maps/documentation/directions)
- [Playground para Rutas](https://www.google.com/maps)

---

**Estado:** ✅ Guía completa lista para implementar
**Próximo paso:** Implementar paso a paso según necesites
