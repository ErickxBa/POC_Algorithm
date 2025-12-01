# 📊 Resumen Completo - Alertify POC

## Descripción del Proyecto

**Alertify** es una prueba de concepto (POC) de un sistema de ruteo dinámico inteligente que utiliza el **algoritmo LPA* (Lifelong Planning A*)** para calcular rutas óptimas considerando tanto distancia como nivel de riesgo de las calles, con actualizaciones en tiempo real.

## 🎯 Objetivo

Demostrar la aplicabilidad del algoritmo LPA* en un sistema real de ruteo urbano donde:
- Los costos de las rutas cambian dinámicamente (riesgos por incidentes)
- La replanificación es incremental (solo actualiza nodos afectados)
- Los usuarios pueden elegir perfiles de seguridad personalizados
- Las actualizaciones se comunican en tiempo real

## 📱 Stack Tecnológico

### Frontend (Android)
- **Lenguaje**: Kotlin
- **Arquitectura**: MVVM
- **Cliente HTTP**: Retrofit 2
- **Comunicación Real-Time**: WebSocket/Socket.io
- **Mapping**: Google Maps API
- **State Management**: StateFlow, ViewModel

### Backend (API)
- **Framework**: NestJS
- **Lenguaje**: TypeScript
- **Base de Datos**: SQL Server Graph Database
- **Algoritmo**: LPA* (Custom Implementation)
- **Comunicación**: REST API + WebSocket
- **Caché**: Redis (opcional)

## 📂 Estructura de Carpetas

```
PruebaConceptoAlgoritmoTesis/
│
├── alertify-backend/                  # Backend NestJS
│   ├── src/
│   │   ├── modules/
│   │   │   ├── lpa/                  # Módulo LPA*
│   │   │   ├── graph/                # Módulo Graph
│   │   │   └── incidents/            # Módulo Incidents
│   │   ├── shared/
│   │   │   ├── database/
│   │   │   ├── dto/
│   │   │   └── utils/
│   │   ├── app.module.ts
│   │   ├── app.service.ts
│   │   ├── app.controller.ts
│   │   └── main.ts
│   ├── test/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── package.json
│   ├── README.md
│   ├── README_SETUP.md
│   ├── API_SPECIFICATION.md
│   ├── ARCHITECTURE.md
│   └── .env.example
│
├── app/                               # Android App
│   └── src/main/java/com/erickballas/pruebaconceptoalgoritmolpa/
│       ├── model/
│       │   ├── Location.kt
│       │   ├── Street.kt
│       │   ├── Route.kt
│       │   └── IncidentReport.kt
│       ├── view/
│       │   └── [UI Components - por implementar]
│       ├── viewmodel/
│       │   ├── RouteViewModel.kt
│       │   ├── MapViewModel.kt
│       │   └── GraphViewModel.kt
│       ├── repository/
│       │   └── GraphRepository.kt
│       └── service/
│           └── ApiService.kt
│
└── POC_LPA_ROUTING.md                # Documentación general POC
```

## 🔧 Módulos Principales

### 1. **LPA Module** (Backend)
Implementa el algoritmo LPA* de forma completa

**Archivos clave**:
- `lpa-star.ts` (300+ líneas) - Implementación del algoritmo
- `lpa.service.ts` - Servicio de ruteo
- `routing.controller.ts` - Endpoints REST

**Funcionalidades**:
- ✅ Búsqueda de rutas óptimas
- ✅ Replanificación incremental
- ✅ Manejo de costo compuesto (α×distancia + β×riesgo)
- ✅ Caché de heurísticas

### 2. **Graph Module** (Backend)
Gestiona el grafo de la ciudad

**Archivos clave**:
- `graph.service.ts` - Servicio principal
- `graph-database.service.ts` - Acceso a BD

**Funcionalidades**:
- ✅ Carga de grafo desde SQL Server
- ✅ Sincronización de datos
- ✅ Caché en memoria

### 3. **Incidents Module** (Backend)
Procesa reportes de incidentes

**Archivos clave**:
- `incidents.service.ts` - Procesamiento
- `incidents.controller.ts` - Endpoints

**Funcionalidades**:
- ✅ Validación de reportes
- ✅ Cálculo de incremento de riesgo
- ✅ Actualización de costos en BD
- ✅ Disparo de replanificación

### 4. **MVVM Layers** (Android)
Arquitectura cliente

**Model Layer**:
- `Location.kt` - Nodos del grafo
- `Street.kt` - Aristas con riesgo
- `Route.kt` - Rutas calculadas
- `IncidentReport.kt` - Reportes

**ViewModel Layer**:
- `RouteViewModel` - Lógica de rutas
- `MapViewModel` - Lógica del mapa
- `GraphViewModel` - Lógica del grafo

**Repository/Service Layer**:
- `GraphRepository` - Acceso a datos
- `ApiService` - Cliente HTTP

## 🔌 API Endpoints

| Método | Endpoint | Descripción |
|--------|----------|------------|
| GET | `/` | Health check |
| GET | `/status` | Estado del servicio |
| POST | `/api/v1/routing/calculate` | Calcular ruta con LPA* |
| GET | `/api/v1/routing/edge/:id` | Info de calle |
| GET | `/api/v1/routing/node/:id` | Info de nodo |
| POST | `/api/v1/incidents/report` | Reportar incidente |
| GET | `/api/v1/incidents/nearby` | Incidentes cercanos |

## 📊 Flujo Principal

### Flujo 1: Cálculo de Ruta

```
Android                          Backend
  │                               │
  ├─ Selecciona inicio/destino ──►│
  │ y perfil de seguridad         │
  │                               │
  │                    ┌──────────┤
  │                    │ LPA*     │
  │                    │ ejecuta  │
  │◄─ Retorna ruta ────┤          │
  │                    └──────────┤
  │                               │
  └─ Muestra en mapa
```

### Flujo 2: Reporte de Incidente

```
Usuario                Backend                 Android
  │                      │                      │
  ├─ Reporta ───────────►│                      │
  │ incidente            │                      │
  │                      ├─ Procesa             │
  │                      ├─ Actualiza BD        │
  │                      ├─ LPA* replani fica   │
  │                      │                      │
  │                      ├─ WebSocket ──────────┤
  │                      │ "Route updated"      │
  │                      │                      │
  │                      │        ┌─ Recalcula │
  │                      │        │ nueva ruta │
  │                      │        └─ Notifica  │
```

## 💾 Base de Datos

### SQL Server Graph Database Schema

```sql
-- Nodos (Intersecciones)
CREATE TABLE dbo.Intersection (
    node_id BIGINT PRIMARY KEY,
    latitude FLOAT,
    longitude FLOAT,
    name NVARCHAR(255)
) AS NODE;

-- Aristas (Calles)
CREATE TABLE dbo.Street (
    street_id BIGINT PRIMARY KEY,
    distance_meters DECIMAL(10, 2),
    current_risk_score DECIMAL(5, 2),
    speed_limit_kmh INT
) AS EDGE;

-- Reportes
CREATE TABLE dbo.IncidentReport (
    report_id BIGINT PRIMARY KEY,
    street_id BIGINT,
    incident_type NVARCHAR(50),
    severity INT,
    reported_at DATETIME
);
```

## 🚀 Quick Start

### Backend

```bash
# 1. Navegar a carpeta
cd alertify-backend

# 2. Instalar dependencias
npm install

# 3. Configurar entorno
cp .env.example .env
# Editar .env con credenciales SQL Server

# 4. Iniciar
npm run start:dev

# Backend en: http://localhost:3000/api/v1
```

### Android

```bash
# 1. Abrir en Android Studio
# 2. Sincronizar Gradle
# 3. Configurar emulador o dispositivo
# 4. Ejecutar aplicación
```

### Docker

```bash
cd alertify-backend

# Inicia backend + SQL Server + Redis
docker-compose up -d

# Backend en: http://localhost:3000/api/v1
```

## 📈 Función de Costo

$$Costo = (\alpha \times Distancia_{norm}) + (\beta \times Riesgo)$$

**Perfiles predefinidos**:
- **Rápido**: α=0.8, β=0.2 (prioriza distancia)
- **Balanceado**: α=0.5, β=0.5 (balance)
- **Seguro**: α=0.2, β=0.8 (prioriza seguridad)

## 🧪 Testing

```bash
# Tests unitarios
npm run test

# Con cobertura
npm run test:cov

# Watch mode
npm run test:watch

# E2E
npm run test:e2e
```

## 📚 Documentación Completa

1. **POC_LPA_ROUTING.md** - Descripción general de la POC
2. **alertify-backend/README.md** - Guía completa del backend
3. **alertify-backend/README_SETUP.md** - Setup y configuración
4. **alertify-backend/API_SPECIFICATION.md** - Especificación de API
5. **alertify-backend/ARCHITECTURE.md** - Arquitectura del sistema

## ✨ Características Implementadas

✅ Algoritmo LPA* completo (300+ líneas)
✅ Módulo de LPA con servicio y controller
✅ Módulo de Graph Database
✅ Módulo de Incidentes
✅ Modelos Android MVVM completos
✅ ViewModels con lógica
✅ Repository y Service
✅ API REST validada
✅ DTOs y validación
✅ Utilidades geográficas
✅ Tests unitarios
✅ Documentación completa
✅ Docker + docker-compose
✅ Configuration basada en .env

## 🎓 Conceptos Implementados

1. **LPA*** - Algoritmo de búsqueda incremental
2. **Graph Theory** - Representación de ciudad como grafo
3. **Heuristic Search** - A* y variantes
4. **Cost Functions** - Funciones multiobjetivo
5. **Real-time Systems** - WebSocket + eventos
6. **MVVM Architecture** - Separación de responsabilidades
7. **Dependency Injection** - NestJS DI
8. **Database Design** - SQL Server Graph DB
9. **Microservices Patterns** - Módulos independientes
10. **API Design** - REST + DTOs

## 🔮 Próximas Mejoras

### Corto Plazo
- [ ] Implementar UI en Android
- [ ] WebSocket en tiempo real
- [ ] Tests E2E completos
- [ ] Dashboard de monitoreo

### Mediano Plazo
- [ ] Autenticación JWT
- [ ] Multi-modal routing (auto, bici, transporte público)
- [ ] Predicción de tráfico
- [ ] Caché Redis distribuido

### Largo Plazo
- [ ] ML para predicción de incidentes
- [ ] Optimización de flotas
- [ ] GraphQL API
- [ ] Kubernetes deployment

## 🤝 Contribuciones

El proyecto está abierto a mejoras y contribuciones. Areas de oportunidad:

1. Optimizaciones de performance
2. Más tests
3. Documentación adicional
4. UI completamente funcional
5. Integración con más fuentes de datos

## 📄 Licencia

MIT License

## 👨‍💼 Autor

**Erick Ballas**  
Estudiante - Universidad de Cartagena  
Tesis: Sistema de Ruteo Dinámico con LPA*

---

**Última actualización**: 30 de noviembre de 2025  
**Versión**: 1.0.0 (POC)  
**Estado**: ✅ Estructura completa implementada, listo para desarrollo
