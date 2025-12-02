import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as fs from 'fs';
import * as path from 'path';

export interface GraphData {
  nodes: any[];
  edges: any[];
  incidentReports: any[];
}

@Injectable()
export class GraphDatabaseService {
  private readonly logger = new Logger(GraphDatabaseService.name);
  private graphData: GraphData;
  private readonly dataFilePath: string;
  private readonly USE_PERSISTENCE: boolean;

  constructor(private configService: ConfigService) {
    this.USE_PERSISTENCE = this.configService.get('PERSIST_DATA', 'true') === 'true';
    // Usamos ruta absoluta para evitar perder el archivo
    this.dataFilePath = path.resolve(process.cwd(), 'data', 'graph-data.json');

    this.graphData = { nodes: [], edges: [], incidentReports: [] };

    this.ensureDirectoryExists();
    this.loadGraph().then(() => {
        this.logger.log(`✅ Base de datos lista. Archivo: ${this.dataFilePath}`);
    });
  }

  private ensureDirectoryExists() {
    const dir = path.dirname(this.dataFilePath);
    if (!fs.existsSync(dir)) {
      this.logger.log(`📂 Creando carpeta de datos en: ${dir}`);
      fs.mkdirSync(dir, { recursive: true });
    }
  }

  async loadGraph(): Promise<GraphData> {
    if (this.USE_PERSISTENCE && fs.existsSync(this.dataFilePath)) {
      try {
        const raw = fs.readFileSync(this.dataFilePath, 'utf-8');
        this.graphData = JSON.parse(raw);
        this.logger.log(`📚 Datos cargados: ${this.graphData.nodes.length} nodos, ${this.graphData.incidentReports.length} incidentes.`);
      } catch (e) {
        this.logger.error('Error leyendo archivo de datos, iniciando vacío.', e);
      }
    }
    return this.graphData;
  }

  // --- GENERACIÓN DE RED ROBUSTA (CUADRÍCULA 3x3) ---
  async initializeGraphAroundLocation(lat: number, lon: number) {
    this.logger.log(`🏗️ Construyendo ciudad alrededor de: ${lat}, ${lon}`);

    const gridSize = 3; // 3x3 = 9 intersecciones
    const step = 0.0015; // Distancia entre calles (~150m)

    const nodes = [];
    let nodeIdBase = 100;

    // 1. Crear Nodos (Intersecciones)
    for(let x = 0; x < gridSize; x++) {
        for(let y = 0; y < gridSize; y++) {
            nodes.push({
                nodeId: nodeIdBase + (x * 10) + y, // IDs: 100, 101, 110, 111...
                latitude: lat + (x * step),
                longitude: lon + (y * step),
                gridX: x,
                gridY: y
            });
        }
    }

    const edges = [];
    let edgeId = 1;

    // Helper para crear calle de DOBLE SENTIDO
    const addStreet = (n1, n2) => {
        // Ida
        edges.push({
            edgeId: edgeId++,
            fromNodeId: n1.nodeId,
            toNodeId: n2.nodeId,
            distanceMeters: 150,
            currentRiskScore: 1.0,
            speedLimitKmh: 30
        });
        // Vuelta (¡CRUCIAL PARA QUE HAYA RUTA!)
        edges.push({
            edgeId: edgeId++,
            fromNodeId: n2.nodeId,
            toNodeId: n1.nodeId,
            distanceMeters: 150,
            currentRiskScore: 1.0,
            speedLimitKmh: 30
        });
    };

    // 2. Conectar Nodos
    for(const node of nodes) {
        // Conectar con vecino derecha
        const right = nodes.find(n => n.gridX === node.gridX && n.gridY === node.gridY + 1);
        if (right) addStreet(node, right);

        // Conectar con vecino arriba
        const top = nodes.find(n => n.gridX === node.gridX + 1 && n.gridY === node.gridY);
        if (top) addStreet(node, top);
    }

    // Limpiamos propiedades temporales
    const cleanNodes = nodes.map(({ gridX, gridY, ...n }) => n);

    // Mantenemos los incidentes antiguos si existen, o iniciamos limpio
    // Para esta POC, mejor limpiar incidentes al cambiar de ciudad para evitar puntos lejanos
    this.graphData = {
        nodes: cleanNodes,
        edges: edges,
        incidentReports: []
    };

    this.saveToFile();
    return this.graphData;
  }

  async addIncidentReport(report: any): Promise<number> {
      const reportId = Date.now();
      this.graphData.incidentReports.push({
          ...report,
          reportId,
          reportedAt: new Date().toISOString()
      });

      this.logger.log(`📝 Incidente guardado. Total: ${this.graphData.incidentReports.length}`);

      // Actualizar riesgo en la calle (arista) más cercana
      // (Lógica simplificada: aumentar riesgo de todas las calles cercanas al incidente)
      // ...

      this.saveToFile();
      return reportId;
  }

  // Métodos estándar
  async getNearbyIncidents(lat: number, lon: number, radius: number): Promise<any[]> {
      return this.graphData.incidentReports;
  }
  async getNodes() { return this.graphData.nodes; }
  async getEdges() { return this.graphData.edges; }

  getGraphStatus() {
      return {
          status: 'loaded',
          nodeCount: this.graphData.nodes.length,
          edgeCount: this.graphData.edges.length,
          incidentCount: this.graphData.incidentReports.length,
          loadedAt: new Date().toISOString()
      };
  }

  // Guardado robusto
  private saveToFile() {
      if (!this.USE_PERSISTENCE) return;
      try {
        this.ensureDirectoryExists();
        fs.writeFileSync(this.dataFilePath, JSON.stringify(this.graphData, null, 2));
        this.logger.debug(`💾 Archivo actualizado.`);
      } catch(e) {
        this.logger.error(`❌ Fallo al guardar en disco`, e);
      }
  }

  // Getters auxiliares
  async getConnectedStreets(id: number) { return this.graphData.edges.filter(e => e.fromNodeId === id); }
  async getNode(id: number) { return this.graphData.nodes.find(n => n.nodeId === id); }
  async getEdge(id: number) { return this.graphData.edges.find(e => e.edgeId === id); }
  async updateStreetRisk(id: number, risk: number) { /* implementar si se requiere */ }
}