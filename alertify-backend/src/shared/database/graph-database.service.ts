import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as fs from 'fs';
import * as path from 'path';

export interface GraphData {
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
    currentRiskScore: number;
    speedLimitKmh: number;
  }>;
  incidentReports: Array<{
    reportId: number;
    streetId: number;
    incidentType: string;
    severity: number;
    latitude: number;
    longitude: number;
    reportedAt: string;
  }>;
}

/**
 * Servicio de Base de Datos Embebida en Memoria
 * Almacena datos en memoria con persistencia opcional en JSON
 * Ideal para desarrollo y pruebas
 */
@Injectable()
export class GraphDatabaseService {
  private readonly logger = new Logger(GraphDatabaseService.name);
  private graphData: GraphData;
  private readonly dataFilePath: string;
  private readonly USE_PERSISTENCE: boolean;

  constructor(private configService: ConfigService) {
    this.USE_PERSISTENCE = this.configService.get('PERSIST_DATA', 'true') === 'true';
    this.dataFilePath = path.join(process.cwd(), 'data', 'graph-data.json');
    this.graphData = this.getDefaultGraphData();
  }

  /**
   * Carga el grafo desde memoria o archivo de persistencia
   */
  async loadGraph(): Promise<GraphData> {
    try {
      if (this.USE_PERSISTENCE && this.fileExists()) {
        this.logger.log('Cargando grafo desde archivo persistente...');
        this.graphData = JSON.parse(
          fs.readFileSync(this.dataFilePath, 'utf-8')
        );
      } else {
        this.logger.log('Usando grafo en memoria (datos por defecto)');
        this.graphData = this.getDefaultGraphData();
        if (this.USE_PERSISTENCE) {
          this.saveToFile();
        }
      }

      this.logger.log(
        `✓ Grafo cargado: ${this.graphData.nodes.length} nodos, ${this.graphData.edges.length} aristas`
      );
      return this.graphData;
    } catch (error) {
      this.logger.error('Error cargando grafo', error);
      this.graphData = this.getDefaultGraphData();
      return this.graphData;
    }
  }

  /**
   * Actualiza el riesgo de una calle
   */
  async updateStreetRisk(
    streetId: number,
    newRiskScore: number
  ): Promise<void> {
    try {
      const edge = this.graphData.edges.find(e => e.edgeId === streetId);
      if (!edge) {
        throw new Error(`Calle ${streetId} no encontrada`);
      }

      edge.currentRiskScore = Math.max(0, Math.min(10, newRiskScore));
      
      this.logger.log(
        `✓ Riesgo de calle ${streetId} actualizado a ${edge.currentRiskScore.toFixed(2)}`
      );

      if (this.USE_PERSISTENCE) {
        this.saveToFile();
      }
    } catch (error) {
      this.logger.error('Error actualizando riesgo de calle', error);
      throw error;
    }
  }

  /**
   * Obtiene las calles conectadas a un nodo
   */
  async getConnectedStreets(nodeId: number): Promise<any[]> {
    try {
      return this.graphData.edges.filter(e => e.fromNodeId === nodeId);
    } catch (error) {
      this.logger.error('Error obteniendo calles conectadas', error);
      throw error;
    }
  }

  /**
   * Agrega un reporte de incidente
   */
  async addIncidentReport(report: {
    streetId: number;
    incidentType: string;
    severity: number;
    latitude: number;
    longitude: number;
  }): Promise<number> {
    try {
      const reportId =
        Math.max(
          0,
          ...this.graphData.incidentReports.map(r => r.reportId)
        ) + 1;

      const newReport = {
        reportId,
        ...report,
        reportedAt: new Date().toISOString()
      };

      this.graphData.incidentReports.push(newReport);

      if (this.USE_PERSISTENCE) {
        this.saveToFile();
      }

      return reportId;
    } catch (error) {
      this.logger.error('Error agregando reporte de incidente', error);
      throw error;
    }
  }

  /**
   * Obtiene incidentes cercanos a una ubicación
   */
  async getNearbyIncidents(
    latitude: number,
    longitude: number,
    radiusMeters: number = 5000
  ): Promise<any[]> {
    try {
      const R = 6371000; // Radio de la Tierra en metros

      return this.graphData.incidentReports.filter(report => {
        const dLat = (report.latitude - latitude) * (Math.PI / 180);
        const dLon = (report.longitude - longitude) * (Math.PI / 180);
        const a =
          Math.sin(dLat / 2) * Math.sin(dLat / 2) +
          Math.cos((latitude * Math.PI) / 180) *
            Math.cos((report.latitude * Math.PI) / 180) *
            Math.sin(dLon / 2) *
            Math.sin(dLon / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        const distance = R * c;

        return distance <= radiusMeters;
      });
    } catch (error) {
      this.logger.error('Error obteniendo incidentes cercanos', error);
      throw error;
    }
  }

  /**
   * Obtiene un nodo por ID
   */
  async getNode(nodeId: number): Promise<any> {
    return this.graphData.nodes.find(n => n.nodeId === nodeId);
  }

  /**
   * Obtiene una arista por ID
   */
  async getEdge(edgeId: number): Promise<any> {
    return this.graphData.edges.find(e => e.edgeId === edgeId);
  }

  /**
   * Retorna todos los nodos
   */
  async getAllNodes(): Promise<any[]> {
    return this.graphData.nodes;
  }

  /**
   * Retorna todas las aristas
   */
  async getAllEdges(): Promise<any[]> {
    return this.graphData.edges;
  }

  /**
   * Limpia los datos de incidentes (para testing)
   */
  async clearIncidents(): Promise<void> {
    this.graphData.incidentReports = [];
    if (this.USE_PERSISTENCE) {
      this.saveToFile();
    }
  }

  /**
   * Guarda los datos a archivo
   */
  private saveToFile(): void {
    try {
      const dir = path.dirname(this.dataFilePath);
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }
      fs.writeFileSync(this.dataFilePath, JSON.stringify(this.graphData, null, 2));
      this.logger.debug(`Datos guardados en ${this.dataFilePath}`);
    } catch (error) {
      this.logger.warn('Error guardando datos a archivo', error);
    }
  }

  /**
   * Verifica si el archivo de persistencia existe
   */
  private fileExists(): boolean {
    return fs.existsSync(this.dataFilePath);
  }

  /**
   * Retorna los datos por defecto del grafo
   */
  private getDefaultGraphData(): GraphData {
    return {
      nodes: [
        { nodeId: 100, latitude: 10.3932, longitude: -75.4898 },
        { nodeId: 101, latitude: 10.3943, longitude: -75.4895 },
        { nodeId: 102, latitude: 10.3950, longitude: -75.4900 },
        { nodeId: 200, latitude: 10.3955, longitude: -75.4905 },
        { nodeId: 300, latitude: 10.3960, longitude: -75.4910 },
        { nodeId: 400, latitude: 10.3965, longitude: -75.4915 },
        { nodeId: 500, latitude: 10.3970, longitude: -75.4920 }
      ],
      edges: [
        {
          edgeId: 1,
          fromNodeId: 100,
          toNodeId: 101,
          distanceMeters: 123,
          currentRiskScore: 2.5,
          speedLimitKmh: 50
        },
        {
          edgeId: 2,
          fromNodeId: 101,
          toNodeId: 102,
          distanceMeters: 145,
          currentRiskScore: 3.0,
          speedLimitKmh: 50
        },
        {
          edgeId: 3,
          fromNodeId: 102,
          toNodeId: 200,
          distanceMeters: 156,
          currentRiskScore: 1.5,
          speedLimitKmh: 60
        },
        {
          edgeId: 4,
          fromNodeId: 200,
          toNodeId: 300,
          distanceMeters: 234,
          currentRiskScore: 5.0,
          speedLimitKmh: 40
        },
        {
          edgeId: 5,
          fromNodeId: 300,
          toNodeId: 400,
          distanceMeters: 189,
          currentRiskScore: 7.5,
          speedLimitKmh: 30
        },
        {
          edgeId: 6,
          fromNodeId: 400,
          toNodeId: 500,
          distanceMeters: 201,
          currentRiskScore: 2.0,
          speedLimitKmh: 50
        },
        {
          edgeId: 7,
          fromNodeId: 101,
          toNodeId: 200,
          distanceMeters: 267,
          currentRiskScore: 4.0,
          speedLimitKmh: 50
        },
        {
          edgeId: 8,
          fromNodeId: 200,
          toNodeId: 400,
          distanceMeters: 445,
          currentRiskScore: 2.5,
          speedLimitKmh: 50
        }
      ],
      incidentReports: []
    };
  }
}
