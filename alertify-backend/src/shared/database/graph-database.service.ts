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
    this.dataFilePath = path.join(process.cwd(), 'data', 'graph-data.json');
    // Inicializamos vacío o cargamos si existe
    this.graphData = { nodes: [], edges: [], incidentReports: [] };
  }

  async loadGraph(): Promise<GraphData> {
    if (this.USE_PERSISTENCE && fs.existsSync(this.dataFilePath)) {
      this.graphData = JSON.parse(fs.readFileSync(this.dataFilePath, 'utf-8'));
    } else {
        // Si no hay datos, iniciamos con una lista vacía esperando que el usuario inicialice
        this.logger.log('Esperando inicialización de ubicación...');
    }
    return this.graphData;
  }

  // --- NUEVA FUNCIÓN MÁGICA: Genera la ciudad donde tú estés ---
  async initializeGraphAroundLocation(lat: number, lon: number) {
    this.logger.log(`Generando grafo alrededor de: ${lat}, ${lon}`);

    // Generar 7 nodos alrededor de tu ubicación (simulando cuadras)
    // Desplazamiento aprox: 0.001 grados ~ 111 metros
    const delta = 0.0015;

    const nodes = [
      { nodeId: 100, latitude: lat, longitude: lon }, // Tú estás aquí (Centro)
      { nodeId: 101, latitude: lat + delta, longitude: lon }, // Norte
      { nodeId: 102, latitude: lat + delta, longitude: lon + delta }, // Noreste
      { nodeId: 200, latitude: lat, longitude: lon + delta }, // Este
      { nodeId: 300, latitude: lat - delta, longitude: lon + delta }, // Sureste
      { nodeId: 400, latitude: lat - delta, longitude: lon }, // Sur
      { nodeId: 500, latitude: lat - delta, longitude: lon - delta }  // Suroeste
    ];

    const edges = [
      { edgeId: 1, fromNodeId: 100, toNodeId: 101, distanceMeters: 150, currentRiskScore: 1.0, speedLimitKmh: 30 },
      { edgeId: 2, fromNodeId: 101, toNodeId: 102, distanceMeters: 150, currentRiskScore: 2.0, speedLimitKmh: 30 },
      { edgeId: 3, fromNodeId: 102, toNodeId: 200, distanceMeters: 150, currentRiskScore: 1.0, speedLimitKmh: 40 },
      { edgeId: 4, fromNodeId: 200, toNodeId: 300, distanceMeters: 150, currentRiskScore: 3.0, speedLimitKmh: 40 },
      { edgeId: 5, fromNodeId: 300, toNodeId: 400, distanceMeters: 150, currentRiskScore: 1.0, speedLimitKmh: 30 },
      { edgeId: 6, fromNodeId: 400, toNodeId: 500, distanceMeters: 150, currentRiskScore: 1.0, speedLimitKmh: 30 },
      // Conexiones al centro
      { edgeId: 7, fromNodeId: 100, toNodeId: 200, distanceMeters: 150, currentRiskScore: 1.5, speedLimitKmh: 30 },
      { edgeId: 8, fromNodeId: 100, toNodeId: 400, distanceMeters: 150, currentRiskScore: 1.0, speedLimitKmh: 30 }
    ];

    this.graphData = {
      nodes,
      edges,
      incidentReports: []
    };

    if (this.USE_PERSISTENCE) this.saveToFile();
    return this.graphData;
  }

  // ... (MANTÉN EL RESTO DE MÉTODOS IGUALES: updateStreetRisk, addIncidentReport, etc.) ...

  async updateStreetRisk(streetId: number, newRiskScore: number): Promise<void> {
      const edge = this.graphData.edges.find(e => e.edgeId === streetId);
      if (edge) {
          edge.currentRiskScore = Math.max(0, Math.min(10, newRiskScore));
          if (this.USE_PERSISTENCE) this.saveToFile();
      }
  }

  async addIncidentReport(report: any): Promise<number> {
      const reportId = Date.now();
      this.graphData.incidentReports.push({ ...report, reportId, reportedAt: new Date().toISOString() });
      if (this.USE_PERSISTENCE) this.saveToFile();
      return reportId;
  }

  async getNearbyIncidents(lat: number, lon: number, radius: number): Promise<any[]> {
      return this.graphData.incidentReports; // Para POC simplificada devolvemos todos
  }

  async getConnectedStreets(nodeId: number) { return this.graphData.edges.filter(e => e.fromNodeId === nodeId); }
  async getNodes() { return this.graphData.nodes; }
  async getEdges() { return this.graphData.edges; }
  async getNode(id: number) { return this.graphData.nodes.find(n => n.nodeId === id); }
  async getEdge(id: number) { return this.graphData.edges.find(e => e.edgeId === id); } // FIXED: added getEdge

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
      fs.writeFileSync(this.dataFilePath, JSON.stringify(this.graphData, null, 2));
  }
}