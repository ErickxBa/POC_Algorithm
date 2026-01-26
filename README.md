# Alertify - Sistema de Rutas Seguras con Algoritmo LPA

> Una aplicación Android que utiliza el algoritmo A* (LPA - Lifelong Planning A*) para calcular rutas seguras en ciudades, considerando riesgo y distancia.

## 🎯 Características Principales

- **🗺️ Mapa Interactivo**: Visualización en tiempo real con OSMDroid
- **🛣️ Cálculo de Rutas Inteligente**: Algoritmo A* con perfiles de seguridad
- **📍 GPS en Tiempo Real**: Detección de ubicación del usuario
- **⚠️ Reporte de Incidentes**: Sistema de reporte de peligros en la vía
- **📊 Visualización de Grafo**: Nodos y aristas de la red vial
- **🔍 Búsqueda de Direcciones**: Integración con Nominatim/OpenStreetMap
- **🎨 UI Moderna**: Jetpack Compose con Material3

## 🏗️ Arquitectura

### Frontend (Android)
- **Framework**: Jetpack Compose
- **Mapas**: OSMDroid 6.1.18
- **Networking**: Retrofit 2.9.0
- **Ubicación**: Google Play Services Location
- **Estado**: Kotlin Coroutines + StateFlow

### Backend (NestJS)
- **Framework**: NestJS
- **Algoritmo**: A* con Lifelong Planning
- **Persistencia**: JSON File-based
- **API**: RESTful con DTOs validados
- **Deployment**: Docker

## 📋 Requisitos

### Dispositivo/Emulador
- Android API 24+ (Nougat)
- 100 MB de espacio
- Conexión a internet
- GPS o simular ubicación

### Desarrollo
- Android Studio Jellyfish+
- Node.js 18+
- Docker & Docker Compose
- Kotlin 1.9+

## 🚀 Inicio Rápido

### 1. Backend
```bash
cd alertify-backend
docker-compose up -d
# Verifica: curl http://localhost:3000/api/v1/graph/status
```

### 2. Frontend
```bash
# Abre en Android Studio
# Build > Build APK
# Run en emulador o dispositivo
```

### 3. Permisos
La app solicita:
- `ACCESS_FINE_LOCATION` - GPS
- `ACCESS_COARSE_LOCATION` - Ubicación aproximada

## 📱 Pantallas

### 1. **Mapa** (MapScreen.kt)
- Visualización OSMDroid
- Marcador de usuario (azul)
- Polylines de rutas (azul)
- Zonas de incidentes (rojo)
- Botones de control

### 2. **Grafo** (GraphScreen.kt)
- Nodos de intersecciones
- Aristas de calles (polylines negras)
- Zoom interactivo
- Coordenadas dinámicas

### 3. **Planificador de Rutas** (RoutePlanningScreen.kt)
- Búsqueda de destino
- Autocompletado Nominatim
- Selección de perfil (Rápida/Equilibrada/Segura)
- Integración con mapa

### 4. **Reporte de Incidentes** (ReportIncidentScreen.kt)
- Búsqueda de ubicación
- Tipo de incidente
- Severidad (1-10)
- Descripción opcional

### 5. **Home** (HomeScreen.kt)
- Menú principal
- Acceso a todas las pantallas
- Información de seguridad

## 🔌 API Endpoints

### Grafo
```
POST   /graph/initialize          - Inicializar grafo
GET    /graph/status              - Estado del grafo
```

### Rutas
```
POST   /routing/calculate          - Calcular ruta
```

### Incidentes
```
POST   /incidents/report           - Reportar incidente
GET    /incidents/nearby           - Incidentes cercanos
```

## 🗂️ Estructura del Proyecto

```
alertify-backend/
├── src/
│   ├── modules/
│   │   ├── graph/        - Gestión de grafo
│   │   ├── routing/      - Cálculo de rutas (A*)
│   │   └── incidents/    - Reportes
│   ├── shared/
│   │   ├── database/     - Persistencia
│   │   ├── utils/        - Funciones auxiliares
│   │   └── dto/          - Data Transfer Objects
│   └── main.ts

app/
├── src/main/java/com/erickballas/pruebaconceptoalgoritmolpa/
│   ├── view/             - Pantallas Compose
│   ├── viewmodel/        - Estados reactivos
│   ├── service/          - Servicios (API, Nominatim)
│   ├── repository/       - Acceso a datos
│   ├── model/            - Modelos de datos
│   └── MainActivity.kt   - Navegación
```

## 🔧 Configuración

### Backend
Archivo: `alertify-backend/.env`
```env
NODE_ENV=development
PERSIST_DATA=true
PORT=3000
```

### Frontend
Archivo: `app/src/main/java/.../service/RetrofitClient.kt`
```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/api/v1/"
```

## 📊 Algoritmo A*

```
- Cálculo de costo: F = G + H
- G: Costo acumulado (distancia × α + riesgo × β)
- H: Heurística (distancia Euclidiana al destino)
- Perfiles:
  * FASTEST:  α=0.8, β=0.2 (prioriza distancia)
  * BALANCED: α=0.5, β=0.5 (equilibrado)
  * SAFEST:   α=0.2, β=0.8 (prioriza seguridad)
```

## ✅ Testing

### Verificar Polylines
```
1. Calcular ruta → Línea AZUL en mapa
2. Ver grafo → Líneas NEGRAS entre nodos
3. Reportar incidente → Círculo ROJO en mapa
```

### Verificar Backend
```bash
# Inicializar grafo
curl -X POST http://localhost:3000/api/v1/graph/initialize \
  -H "Content-Type: application/json" \
  -d '{"latitude": -0.1807, "longitude": -78.4678}'

# Calcular ruta
curl -X POST http://localhost:3000/api/v1/routing/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "startNodeId": 100,
    "goalNodeId": 111,
    "safetyProfile": "balanced"
  }'
```

## 📚 Documentación Completa

- **[CORRECCIONES.md](CORRECCIONES.md)** - Detalle técnico de correcciones
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Guía de testing
- **[QUICK_START.md](QUICK_START.md)** - Inicio rápido (5 minutos)
- **[RESUMEN_EJECUTIVO.md](RESUMEN_EJECUTIVO.md)** - Resumen de cambios

## 🐛 Issues Conocidos y Soluciones

| Problema | Solución |
|----------|----------|
| Polyline no visible | Verifica que `mapState.route` no está vacío |
| GPS no funciona | Activa Location en Extended Controls (emulador) |
| Backend no responde | `docker-compose logs alertify-backend` |
| Nominatim lento | Espera 2s o verifica conexión internet |
| Color incorrecto | Usa `android.graphics.Color` para Paint |

## 🎓 Conceptos Implementados

- [x] Jetpack Compose
- [x] MVVM + StateFlow
- [x] Retrofit + Coroutines
- [x] Android Lifecycle
- [x] Algoritmo A*
- [x] Cálculo Haversine
- [x] OSMDroid overlays
- [x] Navigation Compose
- [x] Material3 Design
- [x] ViewModel Factory

## 📈 Rendimiento

- **Mapa**: 60 FPS con 100+ objetos
- **Cálculo de ruta**: < 2s para grafo 5×5
- **Renderizado polyline**: Instantáneo
- **Búsqueda Nominatim**: 1-2s

## 🔐 Privacidad y Seguridad

- ✅ Ubicación en dispositivo (no se envía sin permiso)
- ✅ HTTPS recomendado en producción
- ✅ Validación de inputs en backend
- ✅ DTOs con `class-validator`

## 🤝 Contribuciones

El proyecto es parte de una tesis de maestría en Informática.

## 📄 Licencia

Proyecto académico - Universidad Central del Ecuador

## 👨‍💼 Autor

**Erick Ballas**  
Ingeniero en Informática  
Universidad Central del Ecuador

---

**Estado**: ✅ Funcional  
**Última actualización**: Enero 2026  
**Versión**: 1.0.0

