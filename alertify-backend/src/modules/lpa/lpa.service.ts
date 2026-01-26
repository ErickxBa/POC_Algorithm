import { Injectable, Logger } from '@nestjs/common';
import { GraphDatabaseService } from '../../shared/database/graph-database.service';

@Injectable()
export class LPAService {
  private readonly logger = new Logger(LPAService.name);

  constructor(private graphDatabaseService: GraphDatabaseService) {}

  // --- MÉTODO FALTANTE QUE CAUSABA EL ERROR ---
  private calculateHeuristic(nodeA: any, nodeB: any): number {
    if (!nodeA || !nodeB) return 0;
    // Distancia Euclidiana simple como heurística
    const dLat = nodeB.latitude - nodeA.latitude;
    const dLon = nodeB.longitude - nodeA.longitude;
    // Factor arbitrario para escala geográfica (grados a "metros aprox" para el score)
    return Math.sqrt(dLat * dLat + dLon * dLon) * 100000;
  }

  async calculateRoute(startNodeId: number, goalNodeId: number, alpha: number, beta: number) {
    this.logger.log(`🔍 Calculando ruta A*: ${startNodeId} -> ${goalNodeId} (α:${alpha}, β:${beta})`);

    const graphData = await this.graphDatabaseService.loadGraph();
    // Mapa rápido para buscar nodos por ID
    const nodesMap = new Map(graphData.nodes.map(n => [n.nodeId, n]));

    // Validar existencia
    if (!nodesMap.has(startNodeId) || !nodesMap.has(goalNodeId)) {
        this.logger.error(`Nodos no encontrados: Inicio=${startNodeId}, Fin=${goalNodeId}`);
        throw new Error('Nodo de inicio o fin no existe en el grafo');
    }

    const goalNode = nodesMap.get(goalNodeId);

    // gScore: Costo real desde el inicio hasta el nodo n
    const gScore = new Map<number, number>();
    // fScore: gScore + Heurística (costo estimado hasta el final)
    const fScore = new Map<number, number>();

    const previous = new Map<number, number>();
    const openSet = new Set<number>(); // Nodos por evaluar

    // Inicialización
    graphData.nodes.forEach(n => {
        gScore.set(n.nodeId, Infinity);
        fScore.set(n.nodeId, Infinity);
    });

    gScore.set(startNodeId, 0);
    fScore.set(startNodeId, this.calculateHeuristic(nodesMap.get(startNodeId), goalNode));
    openSet.add(startNodeId);

    while (openSet.size > 0) {
        // 1. Obtener nodo en openSet con menor fScore
        let current = null;
        let minF = Infinity;

        for (const id of openSet) {
            const score = fScore.get(id);
            if (score < minF) {
                minF = score;
                current = id;
            }
        }

        if (current === null) break;
        if (current === goalNodeId) break; // ¡Llegamos!

        openSet.delete(current);

        // 2. Explorar vecinos
        const neighbors = graphData.edges.filter(e => e.fromNodeId === current);

        for (const edge of neighbors) {
            // --- CORRECCIÓN CRÍTICA: NORMALIZACIÓN ---
            // Normalizamos la distancia (asumiendo 1km = 10 puntos) para que compita con el riesgo (0-10)
            const normalizedDist = Math.min(edge.distanceMeters / 100.0, 10);

            // Costo de arista ponderado
            const edgeCost = (normalizedDist * alpha) + (edge.currentRiskScore * beta);

            const tentativeG = gScore.get(current) + edgeCost;

            if (tentativeG < gScore.get(edge.toNodeId)) {
                // Encontramos un mejor camino a este vecino
                previous.set(edge.toNodeId, current);
                gScore.set(edge.toNodeId, tentativeG);

                const h = this.calculateHeuristic(nodesMap.get(edge.toNodeId), goalNode);
                fScore.set(edge.toNodeId, tentativeG + h);

                if (!openSet.has(edge.toNodeId)) {
                    openSet.add(edge.toNodeId);
                }
            }
        }
    }

    // --- RECONSTRUCCIÓN DEL CAMINO ---
    const path = [];
    let u = goalNodeId;

    // Si no llegamos al destino (distancia sigue siendo infinito)
    if (gScore.get(u) === Infinity) {
        this.logger.warn(`❌ No existe ruta posible entre ${startNodeId} y ${goalNodeId}`);
        throw new Error('No existe una ruta posible entre estos nodos');
    }

    while (u !== undefined) {
        path.unshift(u);
        u = previous.get(u);
        // Seguridad para evitar bucles infinitos si algo falla en el mapa previous
        if (u === undefined && path[0] !== startNodeId) {
             break;
        }
    }

    this.logger.log(`✅ Ruta encontrada: ${path.length} pasos. Costo total: ${gScore.get(goalNodeId).toFixed(2)}`);

    return {
        routeId: Date.now().toString(),
        path: path, // Array de IDs [100, 101, 111...]
        totalCost: gScore.get(goalNodeId),
        totalDistance: 0, // Opcional: podrías sumar edge.distanceMeters aquí si lo necesitas en la UI
        expandedNodes: 0,
        calculationTime: 0,
        description: `Ruta optimizada (${path.length} pasos)`
    };
  }
}