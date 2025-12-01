# Base de Datos Embebida - Guía de Implementación

## 📌 Descripción

La base de datos ahora está **embebida en memoria** en lugar de usar SQL Server externo. Esto simplifica significativamente el despliegue y la configuración.

### Características

- ✅ **Sin dependencias externas** - No requiere SQL Server
- ✅ **Persistencia opcional** - Guarda en JSON si `PERSIST_DATA=true`
- ✅ **Datos en memoria** - Acceso rápido durante la sesión
- ✅ **Fácil de testear** - Sin infraestructura compleja
- ✅ **Ligero** - Ideal para POC y desarrollo

## 🔧 Configuración

### Variables de Entorno (.env)

```env
# Base de Datos Embebida
PERSIST_DATA=true                    # Guardar en JSON después de cambios
DATA_FILE_PATH=./data/graph-data.json # Ubicación del archivo de datos

# Parámetros del Algoritmo
LPA_ALPHA=0.5    # Peso de distancia (0-1)
LPA_BETA=0.5     # Peso de riesgo (0-1)
```

### Archivo de Persistencia

Ubicación: `./data/graph-data.json`

Ejemplo:
```json
{
  "nodes": [
    {
      "nodeId": 100,
      "latitude": 10.3932,
      "longitude": -75.4898
    }
  ],
  "edges": [
    {
      "edgeId": 1,
      "fromNodeId": 100,
      "toNodeId": 101,
      "distanceMeters": 123,
      "currentRiskScore": 2.5,
      "speedLimitKmh": 50
    }
  ],
  "incidentReports": []
}
```

## 🏗️ Arquitectura del Servicio

### GraphDatabaseService

Ubicación: `src/shared/database/graph-database.service.ts`

**Responsabilidades:**
- Cargar datos en startup
- Actualizar riesgos en tiempo real
- Persistir cambios en JSON (opcional)
- Consultar nodos y aristas

**Métodos principales:**

```typescript
// Carga el grafo desde archivo o datos por defecto
async loadGraph(): Promise<GraphData>

// Actualiza el riesgo de una calle
async updateStreetRisk(streetId: number, newRiskScore: number): Promise<void>

// Agrega un reporte de incidente
async addIncidentReport(report: {...}): Promise<number>

// Obtiene incidentes cercanos
async getNearbyIncidents(latitude, longitude, radiusMeters): Promise<Incident[]>

// Obtiene las calles conectadas a un nodo
async getConnectedStreets(nodeId: number): Promise<Street[]>

// Retorna todos los nodos
async getAllNodes(): Promise<Node[]>

// Retorna todas las aristas
async getAllEdges(): Promise<Edge[]>
```

## 🔄 Flujo de Datos

```
[Cliente HTTP]
     ↓
[LpaService/IncidentsService]
     ↓
[GraphDatabaseService] ← Actualiza en memoria
     ↓
[JSON File] (si PERSIST_DATA=true)
```

### Ejemplo: Reporte de Incidente

```
1. POST /incidents/report
   {
     "streetId": 5,
     "incidentType": "accident",
     "severity": 8,
     "latitude": 10.3965,
     "longitude": -75.4915
   }

2. IncidentsService
   - Valida el reporte
   - Calcula nuevo riesgo: oldRisk + (severity/10 * 5)
   - Llama a graphDb.updateStreetRisk()
   - Llama a lpaService.updateEdgeCost()

3. GraphDatabaseService
   - Actualiza edge.currentRiskScore en memoria
   - Si PERSIST_DATA=true, guarda en JSON

4. LPA* Service
   - Recalcula el árbol de costos
   - Retorna nuevas rutas afectadas
```

## 📊 Estructura de Datos

### Interfaz GraphData

```typescript
interface GraphData {
  nodes: Array<{
    nodeId: number;
    latitude: number;
    longitude: number;
  }>;
  
  edges: Array<{
    edgeId: number;
    fromNodeId: number;
    toNodeId: number;
    distanceMeters: number;
    currentRiskScore: number;      // 0-10
    speedLimitKmh: number;
  }>;
  
  incidentReports: Array<{
    reportId: number;
    streetId: number;
    incidentType: string;
    severity: number;              // 1-10
    latitude: number;
    longitude: number;
    reportedAt: string;            // ISO datetime
  }>;
}
```

### Tipos de Incidentes

```typescript
enum IncidentType {
  ACCIDENT = 'accident',
  CONGESTION = 'congestion',
  ROAD_WORK = 'road_work',
  HAZARD = 'hazard',
  DISABLED_VEHICLE = 'disabled_vehicle'
}
```

## 🚀 Iniciar el Backend

### Opción 1: Desarrollo (recomendado)

```bash
npm install
npm run start:dev
```

Servidor en: `http://localhost:3000`

### Opción 2: Producción

```bash
npm run build
npm run start:prod
```

### Opción 3: Docker

```bash
docker-compose up -d

# Ver logs
docker-compose logs -f alertify-backend

# Detener
docker-compose down
```

## 🧪 Testing

### Probar la carga del grafo

```bash
curl http://localhost:3000/api/v1/graph/status
```

Respuesta:
```json
{
  "status": "loaded",
  "nodeCount": 7,
  "edgeCount": 8,
  "incidentCount": 0,
  "loadedAt": "2024-01-15T10:30:00Z"
}
```

### Calcular ruta

```bash
curl -X POST http://localhost:3000/api/v1/routing/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "startNodeId": 100,
    "goalNodeId": 500,
    "safetyProfile": "balanced"
  }'
```

### Reportar incidente

```bash
curl -X POST http://localhost:3000/api/v1/incidents/report \
  -H "Content-Type: application/json" \
  -d '{
    "streetId": 5,
    "incidentType": "accident",
    "severity": 8,
    "latitude": 10.3965,
    "longitude": -75.4915
  }'
```

## 💾 Migración a Base de Datos Real

Si en el futuro necesitas migrar a SQL Server, MongoDB, etc.:

1. **No hay cambios en otros servicios** - La abstracción permite cambios transparentes
2. **Solo modifica** `src/shared/database/graph-database.service.ts`
3. **Implementa los mismos métodos** con queries a tu BD elegida
4. **Los controladores y el algoritmo LPA* siguen funcionando igual**

Ejemplo estructura para migración:

```typescript
@Injectable()
export class GraphDatabaseService {
  constructor(private dbConnection: Connection) {}
  
  async loadGraph(): Promise<GraphData> {
    // Cambiar a: SELECT * FROM nodes; SELECT * FROM edges;
  }
  
  async updateStreetRisk(streetId, newRiskScore) {
    // Cambiar a: UPDATE edges SET risk = ? WHERE id = ?
  }
}
```

## 🔐 Consideraciones de Producción

### Datos en Memoria

- ✅ Rápido
- ⚠️ Se pierde al reiniciar (usa PERSIST_DATA=true)
- ⚠️ Limitado por RAM disponible

### Con Persistencia JSON

- ✅ Se guarda automáticamente
- ✅ Fácil de respaldar
- ⚠️ Más lento que BD relacional
- ⚠️ No escalable para millones de nodos/aristas

### Para Producción Real

Recomendamos:
1. **PostgreSQL con PostGIS** - Para datos geoespaciales
2. **Neo4j** - Nativo para grafos
3. **MongoDB** - Para flexibilidad de esquema
4. **SQL Server Graph** - Si ya tienes MSSQL

## 📚 Archivos Relacionados

- `src/shared/database/graph-database.service.ts` - Implementación
- `.env.example` - Variables de configuración
- `docker-compose.yml` - Solo backend
- `data/graph-data.json` - Archivo de persistencia (creado automáticamente)

## ✅ Checklist de Verificación

- [ ] Instalar dependencias: `npm install`
- [ ] Crear `.env`: `cp .env.example .env`
- [ ] Iniciar backend: `npm run start:dev`
- [ ] Probar health check: `curl http://localhost:3000/health`
- [ ] Calcular ruta: `curl -X POST http://localhost:3000/api/v1/routing/calculate ...`
- [ ] Reportar incidente: `curl -X POST http://localhost:3000/api/v1/incidents/report ...`
- [ ] Verificar `data/graph-data.json` se creó
- [ ] Revisar logs para errores

---

**Última actualización:** 2024-01-15  
**Versión:** 1.0 (Base de Datos Embebida)
