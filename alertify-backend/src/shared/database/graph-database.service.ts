import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as fs from 'fs';
import * as path from 'path';

// --- INTERFACES ---
export interface GraphNode {
  nodeId: number;
  latitude: number;
  longitude: number;
}

export interface GraphEdge {
  edgeId: number;
  streetId: number;
  fromNodeId: number;
  toNodeId: number;
  distanceMeters: number;
  currentRiskScore: number;
  speedLimitKmh: number;
  streetName?: string;
}

export interface GraphData {
  nodes: GraphNode[];
  edges: GraphEdge[];
  incidentReports: any[];
}

@Injectable()
export class GraphDatabaseService {
  private readonly logger = new Logger(GraphDatabaseService.name);
  private graphData: GraphData;

  // --- OPTIMIZACIÓN: Índice de Adyacencia en Memoria ---
  // Map<NodeId, List<Edge>> -> Permite buscar vecinos en O(1) en vez de O(N)
  private adjacencyList: Map<number, GraphEdge[]> = new Map();

  private readonly dataFilePath: string;
  private readonly USE_PERSISTENCE: boolean;

  constructor(private configService: ConfigService) {
    this.USE_PERSISTENCE = this.configService.get('PERSIST_DATA', 'true') === 'true';
    this.dataFilePath = path.resolve(process.cwd(), 'data', 'graph-data.json');
    this.graphData = { nodes: [], edges: [], incidentReports: [] };

    this.ensureDirectoryExists();
    this.loadGraph().then(() => {
        this.logger.log(`✅ Base de datos lista. Nodos: ${this.graphData.nodes.length}, Aristas: ${this.graphData.edges.length}`);
    });
  }

  private ensureDirectoryExists() {
    const dir = path.dirname(this.dataFilePath);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  }

  async loadGraph(): Promise<GraphData> {
    if (this.USE_PERSISTENCE && fs.existsSync(this.dataFilePath)) {
      try {
        const raw = fs.readFileSync(this.dataFilePath, 'utf-8');
        this.graphData = JSON.parse(raw);
        this.buildAdjacencyList(); // <--- IMPORTANTE: Construir índice al cargar
      } catch (e) {
        this.logger.error('Error leyendo archivo de datos, iniciando vacío.', e);
      }
    }
    return this.graphData;
  }

  // Construye el índice rápido para el algoritmo de ruteo
  private buildAdjacencyList() {
      this.adjacencyList.clear();
      this.graphData.edges.forEach(edge => {
          if (!this.adjacencyList.has(edge.fromNodeId)) {
              this.adjacencyList.set(edge.fromNodeId, []);
          }
          this.adjacencyList.get(edge.fromNodeId)?.push(edge);
      });
      this.logger.log("⚡ Índice de adyacencia construido para ruteo rápido.");
  }

  // --- CONEXIÓN A OPENSTREETMAP ---
  async initializeGraphAroundLocation(lat: number, lon: number) {
    this.logger.log(`🌍 Descargando mapa real alrededor de: ${lat}, ${lon} ...`);

    // Radio aprox 1km (Delta 0.01)
    const delta = 0.01;
    const query = `
      [out:json][timeout:25];
      (
        way["highway"]( ${lat - delta}, ${lon - delta}, ${lat + delta}, ${lon + delta} );
      );
      out body;
      >;
      out skel qt;
    `;

    try {
        const response = await fetch('https://overpass-api.de/api/interpreter', {
            method: 'POST', body: query
        });

        if (!response.ok) throw new Error(`Error Overpass: ${response.statusText}`);

        const data = await response.json();
        this.processOverpassData(data);
        return this.graphData;

    } catch (error) {
        this.logger.error("❌ Fallo al descargar mapa de OSM", error);
        throw error;
    }
  }

  private processOverpassData(osmData: any) {
      const nodesMap = new Map<number, GraphNode>();
      const edges: GraphEdge[] = [];
      let edgeCounter = 1;

      osmData.elements.forEach((el: any) => {
          if (el.type === 'node') {
              nodesMap.set(el.id, { nodeId: el.id, latitude: el.lat, longitude: el.lon });
          }
      });

      osmData.elements.forEach((el: any) => {
          if (el.type === 'way' && el.nodes && el.nodes.length > 1) {
              for (let i = 0; i < el.nodes.length - 1; i++) {
                  const idA = el.nodes[i];
                  const idB = el.nodes[i+1];

                  if (nodesMap.has(idA) && nodesMap.has(idB)) {
                      const nodeA = nodesMap.get(idA)!;
                      const nodeB = nodesMap.get(idB)!;
                      const dist = this.haversine(nodeA.latitude, nodeA.longitude, nodeB.latitude, nodeB.longitude);

                      const currentId = edgeCounter++;
                      // Ida
                      edges.push({
                          edgeId: currentId, streetId: currentId,
                          fromNodeId: idA, toNodeId: idB,
                          distanceMeters: dist, currentRiskScore: 1.0, speedLimitKmh: 50,
                          streetName: el.tags?.name || "Calle sin nombre"
                      });

                      // Vuelta (Asumimos bidireccional para facilitar movimiento en la POC)
                      const returnId = edgeCounter++;
                      edges.push({
                          edgeId: returnId, streetId: returnId,
                          fromNodeId: idB, toNodeId: idA,
                          distanceMeters: dist, currentRiskScore: 1.0, speedLimitKmh: 50,
                          streetName: el.tags?.name || "Calle sin nombre"
                      });
                  }
              }
          }
      });

      // Filtrar nodos huérfanos
      const usedNodeIds = new Set<number>();
      edges.forEach(e => { usedNodeIds.add(e.fromNodeId); usedNodeIds.add(e.toNodeId); });
      const cleanNodes = Array.from(usedNodeIds).map(id => nodesMap.get(id)!);

      this.graphData = {
          nodes: cleanNodes,
          edges: edges,
          incidentReports: this.graphData.incidentReports
      };

      this.saveToFile();
      this.buildAdjacencyList(); // Actualizar índice
      this.logger.log(`✅ Mapa procesado: ${cleanNodes.length} nodos, ${edges.length} aristas.`);
  }

  // --- MÉTODOS DE ACCESO PÚBLICO ---

  // Obtener vecinos RAPIDAMENTE usando el índice
  getNeighbors(nodeId: number): GraphEdge[] {
      return this.adjacencyList.get(nodeId) || [];
  }

  async getNearbyIncidents(lat: number, lon: number, radius: number) { return this.graphData.incidentReports; }
  async getNodes() { return this.graphData.nodes; }
  async getEdges() { return this.graphData.edges; }

  getGraphStatus() {
      return {
          status: 'loaded',
          nodeCount: this.graphData.nodes.length,
          edgeCount: this.graphData.edges.length,
          incidentCount: this.graphData.incidentReports.length
      };
  }

  private saveToFile() {
      if (!this.USE_PERSISTENCE) return;
      try {
        this.ensureDirectoryExists();
        fs.writeFileSync(this.dataFilePath, JSON.stringify(this.graphData, null, 2));
      } catch(e) { this.logger.error(`❌ Fallo al guardar`, e); }
  }

  private haversine(lat1: number, lon1: number, lat2: number, lon2: number): number {
      const R = 6371e3;
      const φ1 = lat1 * Math.PI/180, φ2 = lat2 * Math.PI/180;
      const Δφ = (lat2-lat1) * Math.PI/180, Δλ = (lon2-lon1) * Math.PI/180;
      const a = Math.sin(Δφ/2) * Math.sin(Δφ/2) + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ/2) * Math.sin(Δλ/2);
      return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  }

  // Métodos de soporte para incidentes
  async addIncidentReport(report: any): Promise<number> {
      const reportId = Date.now();
      this.graphData.incidentReports.push({ ...report, reportId, reportedAt: new Date().toISOString() });
      this.saveToFile();
      return reportId;
  }
  async updateStreetRisk(id: number, risk: number) {
      const edge = this.graphData.edges.find(e => e.edgeId === id || e.streetId === id);
      if (edge) { edge.currentRiskScore = risk; this.saveToFile(); }
  }
  async getConnectedStreets(id: number) { return this.getNeighbors(id); } // Reusar índice
  async getNode(id: number) { return this.graphData.nodes.find(n => n.nodeId === id); }
  async getEdge(id: number) { return this.graphData.edges.find(e => e.edgeId === id); }
}