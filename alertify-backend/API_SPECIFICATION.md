# Especificación de API - Alertify Backend

## Información General

- **Nombre**: Alertify Backend
- **Versión**: 1.0.0
- **Base URL**: `http://localhost:3000/api/v1`
- **Autenticación**: JWT (futuro)
- **Formato**: JSON

## Autenticación

Actualmente sin autenticación. En futuro:

```
Authorization: Bearer <token>
```

## Status Codes

| Code | Descripción |
|------|-------------|
| 200 | OK - Solicitud exitosa |
| 201 | Created - Recurso creado |
| 400 | Bad Request - Datos inválidos |
| 404 | Not Found - Recurso no encontrado |
| 500 | Server Error - Error interno del servidor |

## Endpoints

### 1. Health Check

#### GET /

**Descripción**: Verifica que el servicio esté activo

**Response (200)**:
```json
"¡Bienvenido a Alertify! Sistema de ruteo dinámico con LPA*"
```

#### GET /status

**Descripción**: Obtiene el estado del servicio

**Response (200)**:
```json
{
  "status": "online",
  "service": "Alertify LPA* Routing Engine",
  "version": "1.0.0",
  "graphLoaded": true,
  "timestamp": "2025-11-30T10:30:00.000Z"
}
```

---

### 2. Módulo de Ruteo (LPA*)

#### POST /routing/calculate

**Descripción**: Calcula una ruta óptima entre dos nodos usando LPA*

**Headers**:
```
Content-Type: application/json
```

**Request Body**:
```json
{
  "startNodeId": 100,
  "endNodeId": 500,
  "alpha": 0.5,
  "beta": 0.5
}
```

**Parámetros**:
- `startNodeId` (number): ID del nodo de inicio
- `endNodeId` (number): ID del nodo destino
- `alpha` (number, 0-1): Peso de distancia
- `beta` (number, 0-1): Peso de riesgo

**Response (200)**:
```json
{
  "success": true,
  "data": {
    "path": [100, 101, 102, 200, 300, 400, 500],
    "cost": 45.32,
    "expandedNodes": 12,
    "calculationTime": 2.45,
    "description": "Ruta (Perfil: Balanceada)\nDistancia: 0.91 km\nRiesgo promedio: 3.2/10\nCosto calculado: 45.32"
  },
  "message": "Ruta calculada exitosamente"
}
```

**Errores (400)**:
```json
{
  "success": false,
  "message": "Grafo no inicializado"
}
```

---

#### GET /routing/edge/:edgeId

**Descripción**: Obtiene información detallada de una calle (edge)

**Parámetros Path**:
- `edgeId` (number): ID de la calle

**Response (200)**:
```json
{
  "success": true,
  "data": {
    "id": 5,
    "from": 300,
    "to": 400,
    "distance": 189,
    "currentRiskScore": 7.5
  }
}
```

**Errores (404)**:
```json
{
  "success": false,
  "message": "Arista 999 no encontrada"
}
```

---

#### GET /routing/node/:nodeId

**Descripción**: Obtiene información de una intersección (nodo)

**Parámetros Path**:
- `nodeId` (number): ID del nodo

**Response (200)**:
```json
{
  "success": true,
  "data": {
    "id": 100,
    "latitude": 10.3932,
    "longitude": -75.4898
  }
}
```

**Errores (404)**:
```json
{
  "success": false,
  "message": "Nodo 999 no encontrado"
}
```

---

### 3. Módulo de Incidentes

#### POST /incidents/report

**Descripción**: Reporta un nuevo incidente que afecta el riesgo de una calle

**Request Body**:
```json
{
  "streetId": 5,
  "incidentType": "ACCIDENT",
  "severity": 8,
  "latitude": 10.3932,
  "longitude": -75.4898,
  "description": "Accidente vehicular en la intersección de Calle 30 con Carrera 5"
}
```

**Parámetros**:
- `streetId` (number): ID de la calle afectada
- `incidentType` (string): Tipo de incidente
  - `ACCIDENT`: Accidente vehicular
  - `ROBBERY`: Robo
  - `CONSTRUCTION`: Construcción
  - `HEAVY_TRAFFIC`: Tráfico pesado
  - `FLOODING`: Inundación
  - `ROAD_DAMAGE`: Daño en la calzada
  - `PROTEST`: Protesta
  - `OTHER`: Otro
- `severity` (number, 1-10): Severidad del incidente
- `latitude` (number): Latitud del incidente
- `longitude` (number): Longitud del incidente
- `description` (string): Descripción detallada

**Response (200)**:
```json
{
  "success": true,
  "data": {
    "reportId": "RPT-1701349400000-a1b2c3d4e",
    "streetId": 5,
    "previousRiskScore": 7.5,
    "newRiskScore": 9.0,
    "riskIncrement": 1.5,
    "message": "Riesgo actualizado de 7.50 a 9.00"
  },
  "message": "Incidente reportado y procesado"
}
```

**Errores (400)**:
```json
{
  "success": false,
  "message": "severity debe estar entre 1 y 10"
}
```

---

#### GET /incidents/nearby

**Descripción**: Obtiene incidentes cercanos a una ubicación

**Query Parameters**:
- `latitude` (number, requerido): Latitud
- `longitude` (number, requerido): Longitud
- `radiusMeters` (number, opcional): Radio en metros (default: 5000)

**Ejemplo**:
```
GET /incidents/nearby?latitude=10.3932&longitude=-75.4898&radiusMeters=5000
```

**Response (200)**:
```json
{
  "success": true,
  "data": [
    {
      "reportId": 1,
      "streetId": 5,
      "incidentType": "ACCIDENT",
      "severity": 8,
      "latitude": 10.3932,
      "longitude": -75.4898
    },
    {
      "reportId": 2,
      "streetId": 7,
      "incidentType": "HEAVY_TRAFFIC",
      "severity": 5,
      "latitude": 10.3940,
      "longitude": -75.4905
    }
  ],
  "count": 2,
  "message": "2 incidentes encontrados"
}
```

**Errores (400)**:
```json
{
  "success": false,
  "message": "latitude y longitude deben ser números válidos"
}
```

---

## Perfiles de Seguridad Predefinidos

| Perfil | α | β | Comportamiento |
|--------|---|---|---|
| Más Rápido | 0.8 | 0.2 | Prioriza distancia, tolera riesgo |
| Balanceado | 0.5 | 0.5 | Balance entre distancia y riesgo |
| Más Seguro | 0.2 | 0.8 | Prioriza seguridad, acepta más distancia |

---

## Tipos de Incidentes

```typescript
enum IncidentType {
  ACCIDENT = 'ACCIDENT',           // Accidente vehicular
  ROBBERY = 'ROBBERY',             // Robo
  CONSTRUCTION = 'CONSTRUCTION',   // Construcción
  HEAVY_TRAFFIC = 'HEAVY_TRAFFIC', // Tráfico pesado
  FLOODING = 'FLOODING',           // Inundación
  ROAD_DAMAGE = 'ROAD_DAMAGE',     // Daño en la calzada
  PROTEST = 'PROTEST',             // Protesta
  OTHER = 'OTHER'                  // Otro
}
```

---

## Ejemplos cURL

### Calcular Ruta

```bash
curl -X POST http://localhost:3000/api/v1/routing/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "startNodeId": 100,
    "endNodeId": 500,
    "alpha": 0.5,
    "beta": 0.5
  }'
```

### Reportar Incidente

```bash
curl -X POST http://localhost:3000/api/v1/incidents/report \
  -H "Content-Type: application/json" \
  -d '{
    "streetId": 5,
    "incidentType": "ACCIDENT",
    "severity": 8,
    "latitude": 10.3932,
    "longitude": -75.4898,
    "description": "Accidente vehicular"
  }'
```

### Obtener Incidentes Cercanos

```bash
curl -X GET "http://localhost:3000/api/v1/incidents/nearby?latitude=10.3932&longitude=-75.4898&radiusMeters=5000"
```

---

## Rate Limiting

Actualmente sin limitación. Implementar en futuro:
- 100 requests por minuto por IP

---

## CORS

Habilitado para todos los orígenes (*)

Para producción, cambiar en `src/main.ts`:

```typescript
app.enableCors({
  origin: ['https://yourdomain.com'],
  credentials: true
});
```

---

## Versionado de API

- Versión Actual: `v1`
- Base URL: `/api/v1`
- Futuras versiones: `/api/v2`, `/api/v3`, etc.

---

**Última actualización**: 30 de noviembre de 2025
