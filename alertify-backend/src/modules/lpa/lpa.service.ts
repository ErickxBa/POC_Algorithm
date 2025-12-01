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
  private graphData: GraphData;

  constructor(private graphDatabaseService: GraphDatabaseService) {}

  async initialize() {
    this.graphData = await this.graphDatabaseService.loadGraph();
  }

  calculateRoute(startNodeId: number, goalNodeId: number, alpha: number = 0.5, beta: number = 0.5) {
    const startTime = Date.now();

    try {
      if (!this.graphData) {
        throw new Error('Graph not initialized');
      }

      // Buscar nodos
      const startNode = this.graphData.nodes.find(n => n.nodeId === startNodeId);
      const goalNode = this.graphData.nodes.find(n => n.nodeId === goalNodeId);

      if (!startNode || !goalNode) {
        throw new Error('Start or goal node not found');
      }

      // Algoritmo simple de Dijkstra para esta implementación inicial
      const distances = new Map<number, number>();
      const previous = new Map<number, number>();
      const unvisited = new Set<number>();

      this.graphData.nodes.forEach(node => {
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

        const edges = this.graphData.edges.filter(e => e.fromNodeId === current);
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

      while (current !== undefined) {
        path.unshift(current);
        current = previous.get(current);
      }

      if (path[0] !== startNodeId) {
        throw new Error('No route found');
      }

      const totalCost = distances.get(goalNodeId) || 0;
      const calculationTime = Date.now() - startTime;

      return {
        routeId: `route_${Date.now()}`,
        path,
        totalDistance: path.reduce((sum, nodeId, idx) => {
          if (idx === 0) return 0;
          const edge = this.graphData.edges.find(e => e.fromNodeId === path[idx - 1] && e.toNodeId === nodeId);
          return sum + (edge?.distanceMeters || 0);
        }, 0),
        totalCost,
        expandedNodes: this.graphData.nodes.length,
        calculationTime,
        description: `Route from ${startNodeId} to ${goalNodeId}`,
      };
    } catch (error) {
      this.logger.error('Error calculating route', error);
      throw error;
    }
  }
}
