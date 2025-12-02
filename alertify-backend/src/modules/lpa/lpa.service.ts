import { Injectable, Logger } from '@nestjs/common';
import { GraphDatabaseService } from '../../shared/database/graph-database.service';

@Injectable()
export class LPAService {
  private readonly logger = new Logger(LPAService.name);

  constructor(private graphDatabaseService: GraphDatabaseService) {}

  async calculateRoute(startNodeId: number, goalNodeId: number, alpha: number, beta: number) {
    this.logger.log(`🔍 Calculando ruta: ${startNodeId} -> ${goalNodeId}`);

    const graphData = await this.graphDatabaseService.loadGraph();
    if (!graphData.nodes.length) throw new Error('Grafo vacío. Usa "Crear Red" primero.');

    // Dijkstra Clásico
    const distances = new Map<number, number>();
    const previous = new Map<number, number>();
    const unvisited = new Set<number>();

    // Inicialización
    graphData.nodes.forEach(n => {
        distances.set(n.nodeId, Infinity);
        unvisited.add(n.nodeId);
    });
    distances.set(startNodeId, 0);

    while (unvisited.size > 0) {
        // Encontrar nodo con menor distancia
        let current = null;
        let minDist = Infinity;

        for (const id of unvisited) {
            const d = distances.get(id);
            if (d < minDist) {
                minDist = d;
                current = id;
            }
        }

        if (current === null) break; // No quedan nodos alcanzables
        if (current === goalNodeId) break; // Llegamos

        unvisited.delete(current);

        // Explorar vecinos
        const neighbors = graphData.edges.filter(e => e.fromNodeId === current);
        for (const edge of neighbors) {
            const alt = distances.get(current) + (edge.distanceMeters * alpha + edge.currentRiskScore * beta);
            if (alt < distances.get(edge.toNodeId)) {
                distances.set(edge.toNodeId, alt);
                previous.set(edge.toNodeId, current);
            }
        }
    }

    // Reconstruir camino
    const path = [];
    let u = goalNodeId;
    if (distances.get(u) === Infinity) {
        this.logger.error(`❌ No se encontró camino hacia el nodo ${goalNodeId}`);
        throw new Error('No existe una ruta posible entre estos nodos');
    }

    while (u !== undefined) {
        path.unshift(u);
        u = previous.get(u);
    }

    this.logger.log(`✅ Ruta encontrada con ${path.length} pasos`);

    return {
        routeId: Date.now().toString(),
        path: path,
        totalDistance: distances.get(goalNodeId) * 1000, // Solo referencial
        totalCost: distances.get(goalNodeId),
        expandedNodes: 0,
        calculationTime: 0,
        description: "Ruta óptima encontrada"
    };
  }
}