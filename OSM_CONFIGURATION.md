# 📱 Configuración OSM - Alertify

## ✅ Cambios Realizados para OSM

Ya están todos los archivos configurados para usar **OpenStreetMap (OSM)** en lugar de Google Maps.

### 📂 Archivos Actualizados:

#### 1. **MapViewModel.kt**
- ✅ Usa `GeoLocation` (agnóstica, no depende de ningún provider)
- ✅ `mapState: MapState` contiene rutas e incidentes

#### 2. **MapScreen.kt**
- ✅ Usa `AndroidView` con `MapView` de OSM
- ✅ Dibuja rutas con `Polyline`
- ✅ Dibuja incidentes con `Marker`
- ✅ Ubicación del usuario también en marcador

#### 3. **HomeScreen.kt, RoutePlanningScreen.kt, ReportIncidentScreen.kt, GraphScreen.kt**
- ✅ Todas optimizadas para OSM

---

## 🚀 Instalación & Compilación

### Paso 1: Descargar Dependencias

```bash
cd app
./gradlew build
```

### Paso 2: Ejecutar en Android Studio

1. Abre **Android Studio**
2. File → Open → Selecciona la carpeta raíz
3. Espera a que sincronice Gradle
4. Conecta un dispositivo/emulador
5. Click en **Run** (▶️)

---

## 🗺️ Cómo Funciona OSM

### Inicialización

```kotlin
// En MapScreen.kt
LaunchedEffect(Unit) {
    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
}
```

Esto configura OSM con SharedPreferences de Android.

### Dibujar Mapa

```kotlin
AndroidView(
    factory = { ctx ->
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)  // Estilo de OSM
            setMultiTouchControls(true)               // Zoom con dos dedos
            controller.setZoom(15.0)                  // Zoom inicial
            controller.setCenter(GeoPoint(10.3932, -75.4830))  // Cartagena
        }
    }
)
```

### Dibujar Ruta

```kotlin
val line = Polyline().apply {
    val points = route.map { GeoPoint(it.latitude, it.longitude) }
    setPoints(points)
    outlinePaint.color = android.graphics.Color.BLUE
}
view.overlays.add(line)
```

### Dibujar Marcadores

```kotlin
val marker = Marker(view).apply {
    position = GeoPoint(lat, lng)
    title = "Incidente"
    snippet = "Detalles..."
    setOnMarkerClickListener { _, _ ->
        // Acciones al hacer clic
        true
    }
}
view.overlays.add(marker)
```

---

## 🎨 Personalización

### Cambiar Estilo de Mapa

```kotlin
// En MapScreen.kt, dentro de factory = { ctx ->
setTileSource(TileSourceFactory.MAPNIK)      // Estilo por defecto
setTileSource(TileSourceFactory.USGS_TOPO)   // Topográfico
```

### Cambiar Color de Ruta

```kotlin
outlinePaint.color = android.graphics.Color.RED    // Rojo
outlinePaint.color = android.graphics.Color.GREEN  // Verde
outlinePaint.strokeWidth = 15f                      // Grosor
```

### Cambiar Color de Incidentes

Ya está configurado por severidad:
- 🟡 Amarillo (1-3): Bajo
- 🟠 Naranja (4-6): Medio
- 🔴 Rojo (7-10): Alto

---

## 📝 Notas Importantes

### ✅ Lo que Funciona:

- Mapa interactivo (zoom, pan, rotación)
- Rutas dibujadas como líneas azules
- Incidentes como marcadores
- Ubicación del usuario
- Click en incidentes muestra diálogo

### ⚠️ Limitaciones OSM vs Google Maps:

| Feature | OSM | Google Maps |
|---------|-----|-------------|
| Mapa base | ✅ Gratuito | Pago |
| Rutas | ✅ Polilinea | ✅ Directions API |
| Búsqueda | ❌ No integrado | ✅ Places API |
| Geocoding | ❌ Requiere lib extra | ✅ Geocoding API |
| Tráfico | ❌ No | ✅ Sí |

Si necesitas búsqueda o geocoding, agrega:

```gradle
implementation("org.osmdroid:osmdroid-mapsforge:6.1.18")
```

---

## 🔧 Troubleshooting

### El mapa no carga

```kotlin
// Asegúrate que Configuration está inicializado
LaunchedEffect(Unit) {
    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
}
```

### Los marcadores no aparecen

Verifica que estén agregados a `view.overlays`:

```kotlin
view.overlays.add(marker)
view.invalidate()  // Forzar repintado
```

### Las rutas no se ven

Revisa que `mapState.route` no esté vacío:

```kotlin
if (mapState.route.isNotEmpty()) {
    // Dibujar ruta
}
```

---

## 📦 Dependencias OSM

Ya están agregadas en `build.gradle.kts`:

```gradle
implementation("org.osmdroid:osmdroid-android:6.1.18")
implementation("org.osmdroid:osmdroid-wms:6.1.18")
```

---

**Estado:** ✅ OSM totalmente configurado  
**Próximo paso:** Conectar con backend NestJS para rutas reales
