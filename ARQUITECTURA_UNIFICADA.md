# 🏗️ Estructura Unificada del Proyecto - Para Android Studio

## 📁 Organización del Workspace

```
PruebaConceptoAlgoritmoTesis/
├── app/                           # Módulo Android
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/.../    # Código Kotlin
│   │   │   │   ├── model/         # MVVM Models
│   │   │   │   ├── viewmodel/     # MVVM ViewModels
│   │   │   │   ├── repository/    # Repository Pattern
│   │   │   │   ├── service/       # API Service (Retrofit)
│   │   │   │   └── ui/            # Activities/Fragments
│   │   │   └── res/               # Recursos Android
│   │   ├── androidTest/           # Tests instrumentados
│   │   └── test/                  # Unit tests
│   └── build.gradle.kts           # Gradle del módulo
│
├── alertify-backend/              # Backend NestJS
│   ├── src/
│   │   ├── main.ts                # Entry point
│   │   ├── app.module.ts          # Root module
│   │   ├── app.controller.ts      # Health endpoints
│   │   ├── app.service.ts         # App init
│   │   ├── modules/               # Feature modules
│   │   │   ├── lpa/               # LPA* Algorithm
│   │   │   │   ├── lpa.module.ts
│   │   │   │   ├── lpa.service.ts
│   │   │   │   ├── routing.controller.ts
│   │   │   │   └── lpa-star.ts
│   │   │   ├── graph/             # Graph Management
│   │   │   │   ├── graph.module.ts
│   │   │   │   ├── graph.service.ts
│   │   │   │   └── graph.controller.ts
│   │   │   └── incidents/         # Incident Reports
│   │   │       ├── incidents.module.ts
│   │   │       ├── incidents.service.ts
│   │   │       └── incidents.controller.ts
│   │   └── shared/                # Shared resources
│   │       ├── database/          # BD Embebida
│   │       │   └── graph-database.service.ts
│   │       └── dto/               # DTOs para API
│   │           └── index.ts
│   ├── .env                       # Variables de entorno
│   ├── .env.example               # Template
│   ├── docker-compose.yml         # Docker config
│   ├── package.json               # Dependencias Node
│   ├── tsconfig.json              # Config TypeScript
│   └── README.md                  # Guía setup
│
└── documentation/                 # Documentación
    ├── INDEX_ACTUALIZADO.md
    ├── QUICK_REFERENCE.md
    ├── DATABASE_EMBEDDED.md
    └── ...
```

---

## 🔌 Arquitectura de Capas (Para Android)

### **Layer 1: Data Layer**
```
GraphDatabaseService (En Memoria)
    ↓
data/graph-data.json (Persistencia)
```

### **Layer 2: Domain Layer (Backend Services)**
```
GraphService     → Gestiona el grafo
LPAService       → Algoritmo de ruteo
IncidentsService → Reportes de incidentes
```

### **Layer 3: API Layer (REST Endpoints)**
```
GET    /api/v1/graph/status       → Estado del grafo
GET    /api/v1/graph/nodes        → Obtener nodos
GET    /api/v1/graph/edges        → Obtener aristas
POST   /api/v1/routing/calculate  → Calcular ruta
POST   /api/v1/incidents/report   → Reportar incidente
GET    /api/v1/incidents/nearby   → Incidentes cercanos
```

### **Layer 4: Android Layer (MVVM)**
```
Views (Activities/Fragments)
    ↓
ViewModels (RouteViewModel, MapViewModel)
    ↓
Repository (GraphRepository)
    ↓
API Service (Retrofit HTTP Client)
    ↓
Backend REST API
```

---

## 📡 Flujo de Comunicación (Android → Backend)

### Ejemplo 1: Calcular Ruta

```
User selects start/goal in MapView
    ↓
RouteViewModel.calculateRoute(startId, goalId)
    ↓
GraphRepository.calculateRoute()
    ↓
ApiService.calculateRoute() → POST /api/v1/routing/calculate
    ↓
Backend: RoutingController.calculateRoute()
    ↓
Backend: LPAService.calculateRoute()
    ↓
LPA* Algorithm busca ruta óptima
    ↓
Response: { path, totalDistance, totalCost, ... }
    ↓
RouteViewModel actualiza StateFlow
    ↓
UI recompose y muestra ruta en mapa
```

### Ejemplo 2: Reportar Incidente

```
User reports incident from UI
    ↓
MapViewModel.reportIncident(data)
    ↓
GraphRepository.reportIncident()
    ↓
ApiService.reportIncident() → POST /api/v1/incidents/report
    ↓
Backend: IncidentsController.reportIncident()
    ↓
Backend: IncidentsService.reportIncident()
    ↓
GraphDatabaseService actualiza riesgo
    ↓
Response: { reportId, newRiskScore, ... }
    ↓
Trigger re-planning automático en LPA*
    ↓
Notificación en tiempo real (opcional WebSocket)
```

---

## 🔗 Puntos de Integración

### Backend → Android

**Base URL:**
```kotlin
const val BASE_URL = "http://localhost:3000/api/v1/"
// O: "http://192.168.x.x:3000/api/v1/" (IP de tu PC)
```

**DTOs de Retrofit:**
```kotlin
data class RouteResponse(
    val success: Boolean,
    val data: RouteData
)

data class RouteData(
    val routeId: String,
    val path: List<Int>,
    val totalDistance: Int,
    val totalCost: Double,
    val expandedNodes: Int,
    val calculationTime: Int
)
```

**API Interface:**
```kotlin
interface ApiService {
    @POST("routing/calculate")
    suspend fun calculateRoute(
        @Body request: CalculateRouteRequest
    ): RouteResponse
    
    @POST("incidents/report")
    suspend fun reportIncident(
        @Body request: ReportIncidentRequest
    ): IncidentResponse
    
    @GET("graph/status")
    suspend fun getGraphStatus(): GraphStatusResponse
}
```

---

## 📊 Modelos de Datos Compartidos

### Nodo (Node)
```json
{
  "nodeId": 100,
  "latitude": 10.3932,
  "longitude": -75.4898
}
```

### Arista (Edge)
```json
{
  "edgeId": 1,
  "fromNodeId": 100,
  "toNodeId": 101,
  "distanceMeters": 123,
  "currentRiskScore": 2.5,
  "speedLimitKmh": 50
}
```

### Ruta (Route)
```json
{
  "routeId": "route_12345",
  "path": [100, 101, 102, 200, 300],
  "totalDistance": 1258,
  "totalCost": 215.5,
  "expandedNodes": 12,
  "calculationTime": 45
}
```

### Incidente (Incident)
```json
{
  "reportId": "report_123",
  "streetId": 5,
  "incidentType": "accident",
  "severity": 8,
  "latitude": 10.3965,
  "longitude": -75.4915,
  "reportedAt": "2024-01-15T10:30:00Z"
}
```

---

## 🧵 Estados y Flujos (Android MVVM)

### RouteViewModel States

```kotlin
data class RouteState(
    val isLoading: Boolean = false,
    val route: Route? = null,
    val error: String? = null,
    val safetyProfile: SafetyProfile = SafetyProfile.BALANCED
)

enum class SafetyProfile {
    FASTEST,      // α=0.8, β=0.2
    BALANCED,     // α=0.5, β=0.5
    SAFEST        // α=0.2, β=0.8
}
```

### Ejemplo ViewModel

```kotlin
@HiltViewModel
class RouteViewModel @Inject constructor(
    private val repository: GraphRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(RouteState())
    val state: StateFlow<RouteState> = _state.asStateFlow()
    
    fun calculateRoute(startId: Int, goalId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val result = repository.calculateRoute(startId, goalId)
                _state.update { it.copy(route = result, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
```

---

## 🔄 Ciclo de Desarrollo

### 1. Backend Development
```
Backend corriendo: npm run start:dev
Puerto: 3000
Endpoints disponibles: http://localhost:3000/api/v1/*
```

### 2. Testing Backend (cURL)
```bash
# Test ruta
curl -X POST http://localhost:3000/api/v1/routing/calculate \
  -H "Content-Type: application/json" \
  -d '{...}'

# Test incidente
curl -X POST http://localhost:3000/api/v1/incidents/report \
  -H "Content-Type: application/json" \
  -d '{...}'
```

### 3. Android Development
```
En Android Studio:
- Abrir project: /PruebaConceptoAlgoritmoTesis
- Build → Build Project
- Run → Run 'app'
- Emulator conecta a backend en http://192.168.x.x:3000
```

### 4. Testing Android ↔ Backend
```
- Usar Network Inspector en Android Studio
- Verificar requests/responses
- Debugear con Logcat
```

---

## 📱 Endpoints del Backend

| Método | Ruta | Parámetros | Respuesta |
|--------|------|-----------|----------|
| GET | `/health` | - | `{status: ok}` |
| GET | `/api/v1/graph/status` | - | `{nodeCount, edgeCount, ...}` |
| GET | `/api/v1/graph/nodes` | - | `[{nodeId, lat, lon}, ...]` |
| GET | `/api/v1/graph/edges` | - | `[{edgeId, from, to, distance, risk}, ...]` |
| POST | `/api/v1/routing/calculate` | `{startNodeId, goalNodeId, safetyProfile}` | `{routeId, path, cost, ...}` |
| POST | `/api/v1/incidents/report` | `{streetId, type, severity, lat, lon}` | `{reportId, newRisk, ...}` |
| GET | `/api/v1/incidents/nearby` | `{lat, lon, radiusMeters}` | `[{reportId, ...}, ...]` |

---

## 🔧 Configuración para Android Studio

### Paso 1: Conectar Backend
```kotlin
// En ApiService.kt
companion object {
    const val BASE_URL = "http://192.168.x.x:3000/api/v1/"
    // Cambiar x.x a tu IP local
}
```

### Paso 2: Permisos Necesarios
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### Paso 3: Ejecutar Ambos

**Terminal 1 (Backend):**
```bash
cd alertify-backend
npm run start:dev
```

**Terminal 2 (Android):**
```bash
# En Android Studio
Shift + F10 (Run)
```

---

## 📊 Diagrama de Dependencias

```
Android App
    ├─ Views/Activities
    ├─ ViewModels (StateFlow)
    ├─ Repository
    └─ ApiService (Retrofit)
        ↓
    Backend (NestJS)
        ├─ Controllers
        ├─ Services
        ├─ GraphDatabaseService
        └─ data/graph-data.json
```

---

## 🚀 Workflow de Desarrollo

1. **Iniciar Backend**
   ```bash
   cd alertify-backend && npm run start:dev
   ```

2. **Verificar disponibilidad**
   ```bash
   curl http://localhost:3000/health
   ```

3. **Abrir Android Studio**
   - File → Open → `/PruebaConceptoAlgoritmoTesis`

4. **Actualizar BASE_URL**
   - Cambiar en `ApiService.kt` a tu IP local

5. **Ejecutar app**
   - Run → Run 'app'
   - Seleccionar emulator o dispositivo

6. **Testear endpoints**
   - Usar Network Inspector
   - Verificar requests en Network tab

---

## ✅ Checklist Integración

- [ ] Backend corriendo en puerto 3000
- [ ] Android Studio abierto con proyecto
- [ ] BASE_URL configurada con IP correcta
- [ ] Permisos de internet en AndroidManifest.xml
- [ ] Primera ruta calculada exitosamente
- [ ] Primer incidente reportado exitosamente
- [ ] WebSocket para notificaciones (opcional)
- [ ] Database persistente (data/graph-data.json)

---

**Estado:** ✅ Arquitectura unificada lista para desarrollo  
**Próximo paso:** Integración full Android ↔ Backend
