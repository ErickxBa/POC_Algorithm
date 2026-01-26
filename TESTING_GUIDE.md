# Guía de Testing - Alertify App

## Requisitos Previos
- Android Studio con API 24+ (Nougat)
- Emulador Android o dispositivo físico con GPS
- Backend NestJS corriendo (docker-compose up)
- Permisos de ubicación habilitados

---

## Testing de Compose & Polylines

### 1. **Prueba de Mapa Base**
```
✓ Inicia la app
✓ Verifica que el mapa carga correctamente
✓ Realiza zoom in/out
✓ Verifica que el mapa responda a gestos
```

### 2. **Prueba de GPS y Ubicación**
```
✓ La app pide permisos de ubicación
✓ Después de otorgar permisos, aparece el marcador azul
✓ El mapa se centra automáticamente en la ubicación
✓ El botón "Centrar" (MyLocation) refresca la ubicación
```

### 3. **Prueba de Polyline de Ruta**
```
✓ Ve a "Calcular Ruta"
✓ Ingresa origen (autocompletado Nominatim)
✓ Selecciona destino
✓ Elige perfil de seguridad
✓ Verifica que aparezca una línea AZUL en el mapa
✓ La línea debe conectar los puntos calculados por A*
```

### 4. **Prueba de Polylines del Grafo**
```
✓ Ve a "Ver Gráfo"
✓ Verifica que se visualicen los nodos (puntos)
✓ Verifica que se visualicen las aristas (líneas NEGRAS)
✓ Cada línea debe conectar dos nodos
✓ Zoom in/out debe funcionar correctamente
```

### 5. **Prueba de Incidentes**
```
✓ Ve a "Reportar Incidente"
✓ Busca una dirección
✓ Selecciona un tipo de incidente
✓ Ajusta severidad
✓ Verifica que aparezca una zona roja circular en el mapa
✓ El círculo rojo representa el área de peligro
```

---

## Testing del Backend

### 1. **Inicializar Grafo**
```bash
curl -X POST http://localhost:3000/api/v1/graph/initialize \
  -H "Content-Type: application/json" \
  -d '{"latitude": -0.1807, "longitude": -78.4678}'
```

**Respuesta esperada:**
```json
{
  "success": true,
  "data": {...},
  "nodes": [...],
  "edges": [...]
}
```

### 2. **Calcular Ruta**
```bash
curl -X POST http://localhost:3000/api/v1/routing/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "startNodeId": 100,
    "goalNodeId": 111,
    "safetyProfile": "balanced"
  }'
```

**Respuesta esperada:**
```json
{
  "success": true,
  "data": {
    "routeId": "...",
    "path": [100, 101, 111],
    "totalDistance": 300.5,
    "totalCost": 5.2,
    ...
  }
}
```

### 3. **Reportar Incidente**
```bash
curl -X POST http://localhost:3000/api/v1/incidents/report \
  -H "Content-Type: application/json" \
  -d '{
    "streetId": 1,
    "incidentType": "accident",
    "severity": 8,
    "latitude": -0.1807,
    "longitude": -78.4678,
    "description": "Accidente en la esquina"
  }'
```

### 4. **Obtener Incidentes Cercanos**
```bash
curl "http://localhost:3000/api/v1/incidents/nearby?latitude=-0.1807&longitude=-78.4678&radiusMeters=5000"
```

---

## Troubleshooting

### Problema: Polyline no aparece en el mapa
**Causa:** Los puntos en `setPoints()` están vacíos
**Solución:** 
- Verifica que `mapState.route` no está vacío
- Verifica que `calculateRouteToDestination()` se ejecutó correctamente
- Revisa los logs: `adb logcat | grep "MapViewModel"`

### Problema: El grafo no se visualiza
**Causa:** Los nodos/edges no se cargaron
**Solución:**
- Verifica que el backend está corriendo
- Verifica que `loadGraphData()` retorna datos
- Comprueba los logs en backend: `docker logs alertify-backend`

### Problema: GPS no funciona
**Causa:** Permisos no otorgados o GPS deshabilitado
**Solución:**
- Verifica permisos en Settings > Apps > Alertify
- En emulador: abre Extended Controls y simula ubicación
- En dispositivo físico: habilita Location Services

### Problema: Nominatim no retorna sugerencias
**Causa:** Red lenta o servidor OSM no disponible
**Solución:**
- Verifica conexión a internet
- Aumenta el `delay(800)` en `searchLocation()`
- Usa ubicaciones bien conocidas

### Problema: Color incorrecto en polyline
**Causa:** Confusión entre `android.graphics.Color` y `ComposeColor`
**Solución:**
- Asegúrate de usar `android.graphics.Color.BLUE` para polylines
- Usa `ComposeColor.Blue` solo en Compose elements

---

## Performance Testing

### 1. **Grafo Grande**
- Backend: Generar grafo 5x5 (25 nodos)
- Verificar que no hay lag al renderizar
- Tiempo de cálculo debe ser < 2 segundos

### 2. **Muchos Incidentes**
- Crear 100 incidentes
- Verificar que se renderizan círculos sin lag
- Consultar cercanos debe ser rápido

### 3. **Zoom Extremo**
- Zoom in a nivel máximo
- Zoom out a nivel mínimo
- Verificar que polylines siguen siendo visibles

---

## Checklist de Validación Final

- [x] Compose renderiza sin errores
- [x] Polylines en mapa funcionan
- [x] Polylines en grafo funcionan
- [x] GPS detecta ubicación
- [x] Rutas se calculan correctamente
- [x] Incidentes se reportan correctamente
- [x] Búsqueda de ubicaciones funciona
- [x] Navegación entre pantallas funciona
- [x] Backend responde a todas las peticiones
- [x] Colores y UI se ven correctamente
- [x] Sin errores de compilación
- [x] Sin crashes de runtime

---

## Comandos Útiles

### Compilar App
```bash
cd app
./gradlew assembleDebug
```

### Ver Logs
```bash
adb logcat | grep -E "MapViewModel|GraphViewModel|Polyline|Error"
```

### Limpiar Caché
```bash
./gradlew clean
```

### Ejecutar Backend en Local
```bash
cd alertify-backend
npm install
npm run start:dev
```

---

## Nota Final
Si encuentras algún problema, verifica:
1. Que todos los imports estén correctos
2. Que el backend esté corriendo
3. Que tienes permisos de ubicación
4. Que la red está disponible (para Nominatim)
5. Los logs de AndroidStudio y backend

¡La app debe estar 100% funcional ahora!
