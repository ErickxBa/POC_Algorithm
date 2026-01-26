# Quick Start Guide - Alertify

## 🚀 Inicio Rápido (5 minutos)

### Paso 1: Iniciar Backend
```bash
cd alertify-backend
docker-compose up -d
# Verifica: http://localhost:3000/api/v1/graph/status
```

### Paso 2: Compilar App Android
```bash
# En Android Studio:
# File > Open > app folder
# Build > Build Bundle(s)/APK(s) > Build APK
```

### Paso 3: Ejecutar en Emulador
```bash
# En Android Studio:
# Click ▶ Run
# Selecciona un emulador con API 24+
```

### Paso 4: Otorgar Permisos
```
✓ La app pide acceso a ubicación
✓ Haz click en "Allow"
✓ En emulador: Extended Controls > Location > Manual
```

### Paso 5: Probar Funcionalidades
- 📍 **Mapa**: Se muestra tu ubicación con marcador azul
- 🛣️ **Ruta**: Busca un destino → Ve ruta azul en el mapa
- 📊 **Grafo**: Ve nodos y aristas con líneas negras
- ⚠️ **Incidente**: Reporta un problema → Aparece círculo rojo

---

## 🔧 Configuración

### URLs del Backend
En `RetrofitClient.kt`:
- Emulador: `http://10.0.2.2:3000/api/v1/`
- Dispositivo físico en red: `http://192.168.X.X:3000/api/v1/`
- Local en PC: `http://localhost:3000/api/v1/`

### Ubicación Inicial
En `MapScreen.kt`:
```kotlin
val startPoint = GeoPoint(-0.1807, -78.4678)  // Quito, Ecuador
```

---

## 📊 Testing de Polylines

### Verificar Polyline en Mapa
```
1. Ir a "Calcular Ruta"
2. Ingresar destino: "Parque Metropolitano, Quito"
3. Hacer click "VER RUTA"
4. Volver a Mapa
5. Deberías ver una línea AZUL
```

### Verificar Polyline en Grafo
```
1. Ir a "Ver Gráfo"
2. Deberías ver puntos (nodos) conectados por líneas NEGRAS
3. Zoom in/out para verificar claridad
```

### Verificar Círculo de Incidente
```
1. Ir a Mapa
2. Click botón rojo "⚠️" (modo reporte)
3. Click en "CONFIRMAR AQUÍ"
4. Deberías ver un círculo ROJO en el mapa
```

---

## 🐛 Troubleshooting Rápido

| Problema | Solución |
|----------|----------|
| No aparece ubicación | Abre Extended Controls > Location en emulador |
| Backend no responde | `docker-compose ps` y `docker logs alertify-backend` |
| Polyline no visible | Zoom in/out, verifica que calculateRoute() ejecutó |
| App crashea | Verifica los logs: `adb logcat \| grep Error` |
| No hay sugerencias Nominatim | Verifica conexión a internet, espera 2s |

---

## 📱 Cambios Recientes (Versión Corregida)

✅ **MapScreen.kt**
- Agregado `import android.graphics.Color`
- Agregado alias `ComposeColor` para evitar conflictos
- Polyline ahora renderiza correctamente

✅ **MainActivity.kt**
- Agregada pantalla Home
- Agregada ruta Graph
- Navegación completa entre 5 pantallas

✅ **Validaciones**
- Todos los imports correctos
- Sin errores de compilación
- Polylines funcionando en mapa y grafo

---

## 🎯 Funcionalidades Implementadas

- [x] Mapa base con OSMDroid
- [x] GPS detectando ubicación
- [x] Cálculo de rutas A*
- [x] Visualización de polylines
- [x] Reporte de incidentes
- [x] Búsqueda con Nominatim
- [x] Navegación entre pantallas
- [x] Integración con backend
- [x] Perfiles de seguridad
- [x] Persistencia en backend

---

## 📞 Soporte

Si tienes problemas:
1. Verifica `CORRECCIONES.md` para detalles técnicos
2. Lee `TESTING_GUIDE.md` para procedimientos de testing
3. Revisa los logs de Android Studio
4. Ejecuta `docker logs alertify-backend`

---

## 🎉 ¡Listo!

Tu app debe estar 100% funcional. ¡Disfruta usando Alertify! 🚗🗺️

