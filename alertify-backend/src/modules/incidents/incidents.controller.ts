import { Controller, Post, Get, Body, Query, HttpException, HttpStatus } from '@nestjs/common';
import { IncidentsService } from './incidents.service';

@Controller('incidents')
export class IncidentsController {
  constructor(private readonly incidentsService: IncidentsService) {}

  @Post('report')
  async reportIncident(
    @Body() body: {
      streetId: number;
      incidentType: string;
      severity: number;
      latitude: number;
      longitude: number;
      description?: string;
    }
  ) {
    try {
      const result = await this.incidentsService.reportIncident(
        body.streetId,
        body.incidentType,
        body.severity,
        body.latitude,
        body.longitude,
        body.description
      );

      return {
        success: true,
        message: 'Incidente reportado correctamente',
        data: result, // Esto coincide con IncidentResponse en Android
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message,
        },
        HttpStatus.BAD_REQUEST
      );
    }
  }

  @Get('nearby')
  async getNearbyIncidents(
    // Usamos @Query porque en Android es un GET con parámetros en URL
    @Query('latitude') latitude: string,
    @Query('longitude') longitude: string,
    @Query('radiusMeters') radiusMeters?: string
  ) {
    try {
      // Convertir strings a números
      const lat = parseFloat(latitude);
      const lng = parseFloat(longitude);
      const rad = radiusMeters ? parseFloat(radiusMeters) : 5000;

      const incidents = await this.incidentsService.getNearbyIncidents(lat, lng, rad);

      return {
        success: true,
        message: `${incidents.length} incidentes encontrados`,
        // Importante: Android busca "incidents" o "data"
        incidents: incidents,
        data: incidents
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message,
        },
        HttpStatus.BAD_REQUEST
      );
    }
  }
}