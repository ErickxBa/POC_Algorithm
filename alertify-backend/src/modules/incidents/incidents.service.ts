import { Injectable, Logger } from '@nestjs/common';
import { GraphDatabaseService } from '../../shared/database/graph-database.service';

@Injectable()
export class IncidentsService {
  private readonly logger = new Logger(IncidentsService.name);

  constructor(private graphDatabaseService: GraphDatabaseService) {}

  async reportIncident(streetId: number, incidentType: string, severity: number, latitude: number, longitude: number, description?: string) {
    try {
      const reportId = await this.graphDatabaseService.addIncidentReport({
        streetId,
        incidentType,
        severity,
        latitude,
        longitude,
      });

      // Calcular incremento de riesgo
      const riskIncrement = (severity / 10) * 5;

      // Obtener riesgo anterior
      const edge = await this.graphDatabaseService.getEdge(streetId);
      const previousRiskScore = edge?.currentRiskScore || 0;
      const newRiskScore = Math.min(10, previousRiskScore + riskIncrement);

      // Actualizar riesgo
      await this.graphDatabaseService.updateStreetRisk(streetId, newRiskScore);

      this.logger.log(`✓ Incident reported: ${reportId}, new risk: ${newRiskScore}`);

      return {
        reportId,
        streetId,
        previousRiskScore,
        newRiskScore,
        riskIncrement,
        message: 'Incident reported successfully',
      };
    } catch (error) {
      this.logger.error('Error reporting incident', error);
      throw error;
    }
  }

  async getNearbyIncidents(latitude: number, longitude: number, radiusMeters: number = 5000) {
    try {
      return await this.graphDatabaseService.getNearbyIncidents(latitude, longitude, radiusMeters);
    } catch (error) {
      this.logger.error('Error getting nearby incidents', error);
      throw error;
    }
  }
}
