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
        // this.logger.log(`📚 Datos cargados: ${this.graphData.nodes.length} nodos.`);
      } catch (e) {
        this.logger.error('Error leyendo archivo de datos, iniciando vacío.', e);
      }
    }
    return this.graphData;
  }

  // --- GENERACIÓN DE RED ---
  async initializeGraphAroundLocation(lat: number, lon: number) {
    // CORRECCIÓN PRINCIPAL: Si ya hay datos, NO HACER NADA.
    if (this.graphData.nodes.length > 0) {
        this.logger.log(`♻️ Grafo ya existe (${this.graphData.nodes.length} nodos). Omitiendo reinicio para proteger incidentes.`);
        return this.graphData;
    }

    this.logger.log(`🏗️ Construyendo ciudad NUEVA alrededor de: ${lat}, ${lon}`);

    const gridSize = 3; // 3x3 = 9 intersecciones
    const step = 0.0015; // Distancia entre calles (~150m)

    const nodes = [];
    let nodeIdBase = 100;

    // 1. Crear Nodos
    for(let x = 0; x < gridSize; x++) {
        for(let y = 0; y < gridSize; y++) {
            nodes.push({
                nodeId: nodeIdBase + (x * 10) + y, // IDs: 100, 101, 110...
                latitude: lat + (x * step),
                longitude: lon + (y * step),
                gridX: x,
                gridY: y
            });
        }
    }

    const edges = [];
    let edgeId = 1;

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
        // Vuelta
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
        const right = nodes.find(n => n.gridX === node.gridX && n.gridY === node.gridY + 1);
        if (right) addStreet(node, right);

        const top = nodes.find(n => n.gridX === node.gridX + 1 && n.gridY === node.gridY);
        if (top) addStreet(node, top);
    }

    const cleanNodes = nodes.map(({ gridX, gridY, ...n }) => n);

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
      this.saveToFile();
      return reportId;
  }

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

  private saveToFile() {
      if (!this.USE_PERSISTENCE) return;
      try {
        this.ensureDirectoryExists();
        fs.writeFileSync(this.dataFilePath, JSON.stringify(this.graphData, null, 2));
      } catch(e) {
        this.logger.error(`❌ Fallo al guardar en disco`, e);
      }
  }

  async getConnectedStreets(id: number) { return this.graphData.edges.filter(e => e.fromNodeId === id); }
  async getNode(id: number) { return this.graphData.nodes.find(n => n.nodeId === id); }
  async getEdge(id: number) { return this.graphData.edges.find(e => e.edgeId === id); }

  async updateStreetRisk(id: number, risk: number) {
      const edge = this.graphData.edges.find(e => e.edgeId === id || e.streetId === id);
      if (edge) {
          edge.currentRiskScore = risk;
          this.saveToFile();
      }
  }
}