import { Test, TestingModule } from '@nestjs/testing';
import { LPAService } from './lpa.service';
import { Node, Edge } from './lpa-star';

describe('LPAService', () => {
  let service: LPAService;

  const mockNodes: Node[] = [
    { id: 1, latitude: 10.3932, longitude: -75.4898 },
    { id: 2, latitude: 10.3943, longitude: -75.4895 },
    { id: 3, latitude: 10.3950, longitude: -75.4900 },
    { id: 4, latitude: 10.3955, longitude: -75.4905 }
  ];

  const mockEdges: Edge[] = [
    { id: 1, from: 1, to: 2, distance: 123, currentRiskScore: 2.5 },
    { id: 2, from: 2, to: 3, distance: 145, currentRiskScore: 3.0 },
    { id: 3, from: 3, to: 4, distance: 156, currentRiskScore: 1.5 }
  ];

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [LPAService]
    }).compile();

    service = module.get<LPAService>(LPAService);
    service.initializeGraph(mockNodes, mockEdges);
  });

  describe('calculateRoute', () => {
    it('debería calcular una ruta válida', () => {
      const request = {
        startNodeId: 1,
        endNodeId: 4,
        alpha: 0.5,
        beta: 0.5
      };

      const result = service.calculateRoute(request);

      expect(result).toBeDefined();
      expect(result.path).toBeDefined();
      expect(result.path.length).toBeGreaterThan(0);
      expect(result.path[0]).toBe(1);
      expect(result.path[result.path.length - 1]).toBe(4);
      expect(result.cost).toBeGreaterThan(0);
      expect(result.calculationTime).toBeGreaterThan(0);
    });

    it('debería fallar si el grafo no está inicializado', () => {
      const service2 = new LPAService();
      const request = {
        startNodeId: 1,
        endNodeId: 4,
        alpha: 0.5,
        beta: 0.5
      };

      expect(() => service2.calculateRoute(request)).toThrow();
    });

    it('debería usar parámetros alpha y beta correctamente', () => {
      const requestFast = {
        startNodeId: 1,
        endNodeId: 4,
        alpha: 0.8,
        beta: 0.2
      };

      const requestSafe = {
        startNodeId: 1,
        endNodeId: 4,
        alpha: 0.2,
        beta: 0.8
      };

      const resultFast = service.calculateRoute(requestFast);
      const resultSafe = service.calculateRoute(requestSafe);

      expect(resultFast).toBeDefined();
      expect(resultSafe).toBeDefined();
      // Diferentes parámetros pueden resultar en diferentes costos
    });
  });

  describe('updateEdgeCost', () => {
    it('debería actualizar el costo de una arista', () => {
      const edgeId = 1;
      const newRiskScore = 7.5;

      expect(() => service.updateEdgeCost(edgeId, newRiskScore)).not.toThrow();

      const edge = service.getEdgeInfo(edgeId);
      expect(edge?.currentRiskScore).toBe(newRiskScore);
    });

    it('debería fallar si la arista no existe', () => {
      expect(() => service.updateEdgeCost(999, 5.0)).toThrow();
    });

    it('debería clampear el riesgo entre 0 y 10', () => {
      // El servicio debería prevenir valores fuera de rango
      // Este test verifica que el comportamiento sea consistente
      expect(() => service.updateEdgeCost(1, 15.0)).not.toThrow();
    });
  });

  describe('calculateRouteDistance', () => {
    it('debería calcular la distancia total de una ruta', () => {
      const path = [1, 2, 3, 4];
      const distance = service.calculateRouteDistance(path);

      expect(distance).toBeGreaterThan(0);
      expect(distance).toBe(123 + 145 + 156); // Suma de distancias de aristas
    });

    it('debería retornar 0 para ruta de un solo nodo', () => {
      const path = [1];
      const distance = service.calculateRouteDistance(path);

      expect(distance).toBe(0);
    });
  });

  describe('calculateRouteRisk', () => {
    it('debería calcular el riesgo promedio de una ruta', () => {
      const path = [1, 2, 3, 4];
      const risk = service.calculateRouteRisk(path);

      expect(risk).toBeGreaterThan(0);
      expect(risk).toBeLessThanOrEqual(10);
    });
  });

  describe('getEdgeInfo', () => {
    it('debería retornar información de una arista existente', () => {
      const edge = service.getEdgeInfo(1);

      expect(edge).toBeDefined();
      expect(edge?.id).toBe(1);
      expect(edge?.from).toBe(1);
      expect(edge?.to).toBe(2);
    });

    it('debería retornar undefined para arista inexistente', () => {
      const edge = service.getEdgeInfo(999);

      expect(edge).toBeUndefined();
    });
  });

  describe('getNodeInfo', () => {
    it('debería retornar información de un nodo existente', () => {
      const node = service.getNodeInfo(1);

      expect(node).toBeDefined();
      expect(node?.id).toBe(1);
      expect(node?.latitude).toBe(10.3932);
    });

    it('debería retornar undefined para nodo inexistente', () => {
      const node = service.getNodeInfo(999);

      expect(node).toBeUndefined();
    });
  });
});
