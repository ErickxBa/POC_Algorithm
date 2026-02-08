import { Controller, Post, Get, Body, Query, HttpException, HttpStatus } from '@nestjs/common';
import { ApiOperation, ApiResponse, ApiTags, ApiBody, ApiQuery } from '@nestjs/swagger';
import { IncidentsService } from './incidents.service';

@ApiTags('Incidents')
@Controller('incidents')
export class IncidentsController {
  constructor(private readonly incidentsService: IncidentsService) {}

  @ApiOperation({ summary: 'Reportar un nuevo incidente en la calle' })
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        streetId: { type: 'number', example: 1, description: 'ID de la calle' },
        incidentType: { type: 'string', example: 'robbery', description: 'Tipo de incidente: robbery, accident, police, etc.' },
        severity: { type: 'number', example: 8, description: 'Nivel de severidad (1-10)' },
        latitude: { type: 'number', example: -17.783 },
        longitude: { type: 'number', example: -63.182 },
        description: { type: 'string', example: 'Robo en la esquina', description: 'Descripción opcional del incidente' }
      }
    }
  })
  @ApiResponse({ status: 201, description: 'Incidente reportado correctamente' })
  @ApiResponse({ status: 400, description: 'Error al reportar el incidente' })
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

  @ApiOperation({ summary: 'Obtener incidentes cercanos a una ubicación' })
  @ApiQuery({ name: 'latitude', type: Number, example: -17.783, description: 'Latitud del punto de búsqueda' })
  @ApiQuery({ name: 'longitude', type: Number, example: -63.182, description: 'Longitud del punto de búsqueda' })
  @ApiQuery({ name: 'radiusMeters', type: Number, example: 5000, required: false, description: 'Radio de búsqueda en metros (default: 5000)' })
  @ApiResponse({ status: 200, description: 'Incidentes encontrados correctamente' })
  @ApiResponse({ status: 400, description: 'Error al buscar incidentes' })
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