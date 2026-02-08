import { Injectable, Logger } from '@nestjs/common';
import { GraphDatabaseService } from '../../shared/database/graph-database.service';

@Injectable()
export class LPAService {
  private readonly logger = new Logger(LPAService.name);

  constructor(private graphService: GraphDatabaseService) {}

  private calculateHeuristic(nodeA: any, nodeB: any): number {
    if (!nodeA || !nodeB) return 0;
    const dLat = (nodeB.latitude - nodeA.latitude) * 111320;
    const dLon = (nodeB.longitude - nodeA.longitude) * 111320;
    return Math.sqrt(dLat * dLat + dLon * dLon);
  }

  async calculateRoute(startNodeId: number, goalNodeId: number, alpha: number, beta: number) {
    this.logger.log(`🔍 Calculando ruta: ${startNodeId} -> ${goalNodeId}`);

    const graphData = await this.graphService.loadGraph();
    const nodesMap = new Map(graphData.nodes.map(n => [n.nodeId, n]));

    if (!nodesMap.has(startNodeId) || !nodesMap.has(goalNodeId)) {
        throw new Error('Inicio o Fin no encontrados en el grafo.');
    }

    const goalNode = nodesMap.get(goalNodeId);
    const gScore = new Map<number, number>();
    const fScore = new Map<number, number>();
    const previous = new Map<number, number>();
    const openSet = new Set<number>();

    gScore.set(startNodeId, 0);
    fScore.set(startNodeId, this.calculateHeuristic(nodesMap.get(startNodeId), goalNode));
    openSet.add(startNodeId);

    let steps = 0;
    while (openSet.size > 0 && steps < 15000) {
        steps++;
        let current = null;
        let minF = Infinity;

        for (const id of openSet) {
            const score = fScore.get(id) ?? Infinity;
            if (score < minF) { minF = score; current = id; }
        }

        if (current === null || current === goalNodeId) break;
        openSet.delete(current);

        const neighbors = this.graphService.getNeighbors(current);

        for (const edge of neighbors) {
            const edgeCost = (edge.distanceMeters * alpha) + ((edge.currentRiskScore * 500) * beta);
            const tentativeG = (gScore.get(current) ?? Infinity) + edgeCost;

            if (tentativeG < (gScore.get(edge.toNodeId) ?? Infinity)) {
                previous.set(edge.toNodeId, current);
                gScore.set(edge.toNodeId, tentativeG);
                fScore.set(edge.toNodeId, tentativeG + this.calculateHeuristic(nodesMap.get(edge.toNodeId), goalNode));
                openSet.add(edge.toNodeId);
            }
        }
    }

    const pathCoordinates = [];
    let u = goalNodeId;

    if (!previous.has(u) && u !== startNodeId) {
        throw new Error("No existe ruta entre estos puntos.");
    }

    while (u !== undefined) {
        const node = nodesMap.get(u);
        if (node) {
            pathCoordinates.unshift({
                nodeId: node.nodeId,
                latitude: node.latitude,
                longitude: node.longitude
            });
        }
        u = previous.get(u);
        if (u === undefined && pathCoordinates.length > 0 && pathCoordinates[0].nodeId !== startNodeId) break;
    }

    return {
        routeId: Date.now().toString(),
        path: pathCoordinates, // Devuelve coordenadas reales
        totalCost: gScore.get(goalNodeId) || 0,
        totalDistance: 0,
        description: "Ruta óptima"
    };
  }
}