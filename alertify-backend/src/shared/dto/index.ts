import { IsNumber, IsString, IsOptional, Min, Max } from 'class-validator';

/**
 * ==================== ROUTING ====================
 */

export class CalculateRouteDto {
  @IsNumber()
  startNodeId: number;

  @IsNumber()
  goalNodeId: number;

  @IsOptional()
  @IsString()
  safetyProfile?: 'fastest' | 'balanced' | 'safest';
}

export class RouteResponseDto {
  routeId: string;
  path: number[];
  totalDistance: number;
  totalCost: number;
  expandedNodes: number;
  calculationTime: number;
  description: string;
}

/**
 * ==================== INCIDENTS ====================
 */

export class ReportIncidentDto {
  @IsNumber()
  streetId: number;

  @IsString()
  incidentType: 'accident' | 'congestion' | 'road_work' | 'hazard' | 'disabled_vehicle';

  @IsNumber()
  @Min(1)
  @Max(10)
  severity: number;

  @IsNumber()
  latitude: number;

  @IsNumber()
  longitude: number;

  @IsOptional()
  @IsString()
  description?: string;
}

export class IncidentResponseDto {
  reportId: string;
  streetId: number;
  previousRiskScore: number;
  newRiskScore: number;
  riskIncrement: number;
  message: string;
}

export class NearbyIncidentsRequestDto {
  @IsNumber()
  latitude: number;

  @IsNumber()
  longitude: number;

  @IsOptional()
  @IsNumber()
  radiusMeters?: number;
}

/**
 * ==================== GRAPH ====================
 */

export class GraphStatusDto {
  status: string;
  nodeCount: number;
  edgeCount: number;
  incidentCount: number;
  loadedAt: string;
}

export class NodeDto {
  nodeId: number;
  latitude: number;
  longitude: number;
}

export class EdgeDto {
  edgeId: number;
  fromNodeId: number;
  toNodeId: number;
  distanceMeters: number;
  currentRiskScore: number;
  speedLimitKmh: number;
}

/**
 * ==================== GENERIC ====================
 */

export class ApiResponseDto<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
  timestamp?: string;
}

export class HealthCheckDto {
  status: string;
  service: string;
  timestamp: string;
}
