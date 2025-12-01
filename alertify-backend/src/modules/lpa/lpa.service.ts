import { Injectable, Logger } from '@nestjs/common';
import { GraphDatabaseService, GraphData } from '../../shared/database/graph-database.service';

interface Node {
  id: number;
  g: number;
  rhs: number;
  h: number;
}

interface Edge {
  from: number;
  to: number;
  cost: number;
}

@Injectable()
export class LPAService {
  private readonly logger = new Logger(LPAService.name);

  constructor(private graphDatabaseService: GraphDatabaseService) {}

  async calculateRoute(startNodeId: number, goalNodeId: number, alpha: number = 0.5, beta: number = 0.5) {
    const startTime = Date.now();

    try {
      // 1. Cargar datos frescos
      const graphData = await this.graphDatabaseService.loadGraph();

      if (!graphData || !graphData.nodes || graphData.nodes.length === 0) {
        throw new Error('El grafo no está inicializado. Por favor usa "Crear Red" en la app primero.');
      }

      // Buscar nodos
      const startNode = graphData.nodes.find(n => n.nodeId === startNodeId);
      const goalNode = graphData.nodes.find(n => n.nodeId === goalNodeId);

      if (!startNode || !goalNode) {
        throw new Error(`Nodo inicio (${startNodeId}) o destino (${goalNodeId}) no encontrados.`);
      }

      // Algoritmo Dijkstra
      const distances = new Map<number, number>();
      const previous = new Map<number, number>();
      const unvisited = new Set<number>();

      graphData.nodes.forEach(node => {
        distances.set(node.nodeId, node.nodeId === startNodeId ? 0 : Infinity);
        unvisited.add(node.nodeId);
      });

      while (unvisited.size > 0) {
        let current: number | null = null;
        let minDist = Infinity;

        for (const nodeId of unvisited) {
          const dist = distances.get(nodeId) || Infinity;
          if (dist < minDist) {
            minDist = dist;
            current = nodeId;
          }
        }

        if (current === null || current === goalNodeId) break;

        unvisited.delete(current);

        const edges = graphData.edges.filter(e => e.fromNodeId === current);
        for (const edge of edges) {
          const cost = (alpha * (edge.distanceMeters / 1000)) + (beta * (edge.currentRiskScore / 10));
          const newDist = (distances.get(current) || 0) + cost;

          if (newDist < (distances.get(edge.toNodeId) || Infinity)) {
            distances.set(edge.toNodeId, newDist);
            previous.set(edge.toNodeId, current);
          }
        }
      }

      // Reconstruir ruta
      const path: number[] = [];
      let current: number | undefined = goalNodeId;

      if (distances.get(goalNodeId) === Infinity) {
         throw new Error("No existe una ruta posible entre estos nodos");
      }

      while (current !== undefined) {
        path.unshift(current);
        current = previous.get(current);
      }

      if (path[0] !== startNodeId) {
        throw new Error('No se pudo encontrar una ruta válida');
      }

      const totalCost = distances.get(goalNodeId) || 0;
      const calculationTime = Date.now() - startTime;

      return {
        routeId: `route_${Date.now()}`,
        path,
        totalDistance: path.reduce((sum, nodeId, idx) => {
          if (idx === 0) return 0;
          const edge = graphData.edges.find(e => e.fromNodeId === path[idx - 1] && e.toNodeId === nodeId);
          return sum + (edge?.distanceMeters || 0);
        }, 0),
        totalCost,
        expandedNodes: graphData.nodes.length,
        calculationTime,
        description: `Ruta calculada desde ${startNodeId} a ${goalNodeId}`
      };

    } catch (error) {
      this.logger.error('Error calculating route', error);
      throw error;
    }
  }
}