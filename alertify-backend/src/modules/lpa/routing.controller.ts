import { Controller, Get, Post, Body, HttpException, HttpStatus } from '@nestjs/common';
import { LPAService } from './lpa.service';

@Controller('routing')
export class RoutingController {
  constructor(private readonly lpaService: LPAService) {}

  @Post('calculate')
  async calculateRoute(
    @Body() body: {
      startNodeId: number;
      goalNodeId: number;
      safetyProfile?: string;
    }
  ) {
    try {
      const { startNodeId, goalNodeId, safetyProfile = 'balanced' } = body;

      // Mapear perfil de seguridad a parámetros
      let alpha = 0.5;
      let beta = 0.5;

      switch (safetyProfile) {
        case 'fastest':
          alpha = 0.8;
          beta = 0.2;
          break;
        case 'safest':
          alpha = 0.2;
          beta = 0.8;
          break;
        case 'balanced':
        default:
          alpha = 0.5;
          beta = 0.5;
      }

      const result = this.lpaService.calculateRoute(
        startNodeId,
        goalNodeId,
        alpha,
        beta
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

  @Get('health')
  health() {
    return {
      status: 'ok',
      service: 'routing',
      timestamp: new Date().toISOString(),
    };
  }
}
