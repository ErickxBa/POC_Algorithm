# 📱 Guía de Integración Android ↔ Backend

## 🎯 Objetivo

Conectar la aplicación Android (MVVM) con el backend NestJS via HTTP REST API.

---

## 🔧 Setup Backend (Ya Completado)

```bash
cd alertify-backend
npm install
npm run start:dev
```

✅ Backend corriendo en `http://localhost:3000/api/v1/`

---

## 📡 Setup Android

### 1. Agregar Dependencias (build.gradle.kts)

```kotlin
dependencies {
    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    
    // Dependency Injection (Hilt)
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

### 2. Permisos en AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 3. Crear ApiService (Retrofit)

**Archivo:** `src/main/kotlin/com/erickballas/pruebaconceptoalgoritmolpa/service/ApiService.kt`

```kotlin
import retrofit2.http.*
import kotlinx.coroutines.flow.Flow

interface ApiService {
    companion object {
        const val BASE_URL = "http://192.168.x.x:3000/api/v1/"
        // Cambiar x.x a tu IP local o localhost si emulador
    }

    // ==================== ROUTING ====================
    
    @POST("routing/calculate")
    suspend fun calculateRoute(
        @Body request: CalculateRouteRequest
    ): ApiResponse<RouteData>

    // ==================== GRAPH ====================
    
    @GET("graph/status")
    suspend fun getGraphStatus(): ApiResponse<GraphStatus>

    @GET("graph/nodes")
    suspend fun getGraphNodes(): ApiResponse<List<GraphNode>>

    @GET("graph/edges")
    suspend fun getGraphEdges(): ApiResponse<List<GraphEdge>>

    // ==================== INCIDENTS ====================
    
    @POST("incidents/report")
    suspend fun reportIncident(
        @Body request: ReportIncidentRequest
    ): ApiResponse<IncidentResponse>

    @GET("incidents/nearby")
    suspend fun getNearbyIncidents(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusMeters") radiusMeters: Int = 5000
    ): ApiResponse<List<IncidentData>>

    // ==================== HEALTH ====================
    
    @GET("/health")
    suspend fun healthCheck(): HealthCheck
}

// ==================== REQUEST DTOs ====================

data class CalculateRouteRequest(
    val startNodeId: Int,
    val goalNodeId: Int,
    val safetyProfile: String = "balanced"  // "fastest", "balanced", "safest"
)

data class ReportIncidentRequest(
    val streetId: Int,
    val incidentType: String,  // "accident", "congestion", "road_work", "hazard"
    val severity: Int,  // 1-10
    val latitude: Double,
    val longitude: Double,
    val description: String = ""
)

// ==================== RESPONSE DTOs ====================

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

data class RouteData(
    val routeId: String,
    val path: List<Int>,
    val totalDistance: Int,
    val totalCost: Double,
    val expandedNodes: Int,
    val calculationTime: Int,
    val description: String
)

data class GraphStatus(
    val status: String,
    val nodeCount: Int,
    val edgeCount: Int,
    val incidentCount: Int,
    val loadedAt: String
)

data class GraphNode(
    val nodeId: Int,
    val latitude: Double,
    val longitude: Double
)

data class GraphEdge(
    val edgeId: Int,
    val fromNodeId: Int,
    val toNodeId: Int,
    val distanceMeters: Int,
    val currentRiskScore: Double,
    val speedLimitKmh: Int
)

data class IncidentResponse(
    val reportId: String,
    val streetId: Int,
    val previousRiskScore: Double,
    val newRiskScore: Double,
    val riskIncrement: Double,
    val message: String
)

data class IncidentData(
    val reportId: String,
    val streetId: Int,
    val incidentType: String,
    val severity: Int,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val reportedAt: String
)

data class HealthCheck(
    val status: String,
    val service: String,
    val timestamp: String
)
```

### 4. Configurar Retrofit (RetrofitClient)

**Archivo:** `src/main/kotlin/.../service/RetrofitClient.kt`

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(ApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}
```

### 5. Crear Repository

**Archivo:** `src/main/kotlin/.../repository/GraphRepository.kt`

```kotlin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GraphRepository(
    private val apiService: ApiService
) {
    // Calcular ruta
    fun calculateRoute(
        startNodeId: Int,
        goalNodeId: Int,
        safetyProfile: String = "balanced"
    ): Flow<RouteData> = flow {
        try {
            val request = CalculateRouteRequest(startNodeId, goalNodeId, safetyProfile)
            val response = apiService.calculateRoute(request)
            if (response.success && response.data != null) {
                emit(response.data)
            } else {
                throw Exception(response.message ?: "Error calculando ruta")
            }
        } catch (e: Exception) {
            throw Exception("Error: ${e.message}")
        }
    }

    // Obtener estado del grafo
    suspend fun getGraphStatus(): GraphStatus {
        val response = apiService.getGraphStatus()
        return response.data ?: throw Exception("No graph status available")
    }

    // Obtener nodos
    suspend fun getGraphNodes(): List<GraphNode> {
        val response = apiService.getGraphNodes()
        return response.data ?: emptyList()
    }

    // Obtener aristas
    suspend fun getGraphEdges(): List<GraphEdge> {
        val response = apiService.getGraphEdges()
        return response.data ?: emptyList()
    }

    // Reportar incidente
    suspend fun reportIncident(
        streetId: Int,
        incidentType: String,
        severity: Int,
        latitude: Double,
        longitude: Double,
        description: String = ""
    ): IncidentResponse {
        val request = ReportIncidentRequest(
            streetId, incidentType, severity, latitude, longitude, description
        )
        val response = apiService.reportIncident(request)
        return response.data ?: throw Exception("Error reportando incidente")
    }

    // Obtener incidentes cercanos
    suspend fun getNearbyIncidents(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 5000
    ): List<IncidentData> {
        val response = apiService.getNearbyIncidents(latitude, longitude, radiusMeters)
        return response.data ?: emptyList()
    }
}
```

### 6. ViewModel para Rutas

**Archivo:** `src/main/kotlin/.../viewmodel/RouteViewModel.kt`

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class RouteUIState(
    val isLoading: Boolean = false,
    val route: RouteData? = null,
    val error: String? = null,
    val safetyProfile: String = "balanced"
)

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val repository: GraphRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteUIState())
    val uiState: StateFlow<RouteUIState> = _uiState.asStateFlow()

    fun calculateRoute(startId: Int, goalId: Int, profile: String = "balanced") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.calculateRoute(startId, goalId, profile)
                    .collect { route ->
                        _uiState.update {
                            it.copy(
                                route = route,
                                isLoading = false,
                                safetyProfile = profile
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearRoute() {
        _uiState.update { it.copy(route = null) }
    }
}
```

### 7. ViewModel para Gráfo

**Archivo:** `src/main/kotlin/.../viewmodel/GraphViewModel.kt`

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class GraphUIState(
    val isLoading: Boolean = false,
    val status: GraphStatus? = null,
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val repository: GraphRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphUIState())
    val uiState: StateFlow<GraphUIState> = _uiState.asStateFlow()

    init {
        loadGraphData()
    }

    private fun loadGraphData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val status = repository.getGraphStatus()
                val nodes = repository.getGraphNodes()
                val edges = repository.getGraphEdges()

                _uiState.update {
                    it.copy(
                        status = status,
                        nodes = nodes,
                        edges = edges,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refresh() {
        loadGraphData()
    }
}
```

### 8. ViewModel para Incidentes

**Archivo:** `src/main/kotlin/.../viewmodel/IncidentsViewModel.kt`

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class IncidentsUIState(
    val isLoading: Boolean = false,
    val incidents: List<IncidentData> = emptyList(),
    val lastReportId: String? = null,
    val error: String? = null
)

@HiltViewModel
class IncidentsViewModel @Inject constructor(
    private val repository: GraphRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentsUIState())
    val uiState: StateFlow<IncidentsUIState> = _uiState.asStateFlow()

    fun reportIncident(
        streetId: Int,
        incidentType: String,
        severity: Int,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = repository.reportIncident(
                    streetId, incidentType, severity, latitude, longitude
                )
                _uiState.update {
                    it.copy(
                        lastReportId = response.reportId,
                        isLoading = false
                    )
                }
                // Recargar incidentes cercanos
                loadNearbyIncidents(latitude, longitude)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadNearbyIncidents(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val incidents = repository.getNearbyIncidents(latitude, longitude)
                _uiState.update { it.copy(incidents = incidents) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
```

---

## 📱 Usar en UI

### Ejemplo: Calcular Ruta

```kotlin
@Composable
fun RouteScreen(viewModel: RouteViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    Column {
        if (state.isLoading) {
            CircularProgressIndicator()
        }
        
        state.route?.let { route ->
            Text("Ruta encontrada!")
            Text("Distancia: ${route.totalDistance}m")
            Text("Costo: ${route.totalCost}")
            // Dibujar ruta en mapa...
        }
        
        state.error?.let {
            Text("Error: $it", color = Color.Red)
        }
        
        Button(onClick = {
            viewModel.calculateRoute(100, 500, "balanced")
        }) {
            Text("Calcular Ruta")
        }
    }
}
```

---

## 🔌 Configuración para Emulador

Si usas emulador de Android, el `localhost` no funciona. Necesitas tu IP local:

```bash
# En tu PC, obtén IP local
ipconfig  # Windows
ifconfig  # Mac/Linux

# Busca algo como: 192.168.x.x
```

Actualiza en `ApiService.kt`:
```kotlin
const val BASE_URL = "http://192.168.x.x:3000/api/v1/"
```

---

## ✅ Checklist de Integración

- [ ] Backend corriendo: `npm run start:dev`
- [ ] Dependencias Retrofit agregadas
- [ ] Permisos de INTERNET en AndroidManifest.xml
- [ ] ApiService creado
- [ ] RetrofitClient configurado
- [ ] Repository implementado
- [ ] ViewModels creados
- [ ] BASE_URL configurada con IP correcta
- [ ] Primera ruta calculada exitosamente
- [ ] Primer incidente reportado exitosamente

---

**Estado:** ✅ Integración lista para usar  
**Próximo paso:** Crear UI con Jetpack Compose o XML layouts
