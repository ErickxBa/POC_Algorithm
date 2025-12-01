import { Controller, Post, Get, Body, HttpException, HttpStatus } from '@nestjs/common';
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
        data: result,
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
    @Body() body: { latitude: number; longitude: number; radiusMeters?: number }
  ) {
    try {
      const incidents = await this.incidentsService.getNearbyIncidents(
        body.latitude,
        body.longitude,
        body.radiusMeters || 5000
      );

      return {
        success: true,
        data: incidents,
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

  @Get('health')
  health() {
    return {
      status: 'ok',
      service: 'incidents',
      timestamp: new Date().toISOString(),
    };
  }
}
