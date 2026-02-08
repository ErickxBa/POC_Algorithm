import { Controller, Get, Post, Body, HttpException, HttpStatus } from '@nestjs/common';
import { ApiOperation, ApiResponse, ApiTags, ApiBody } from '@nestjs/swagger';
import { LPAService } from './lpa.service';

@ApiTags('Routing')
@Controller('routing')
export class RoutingController {
  constructor(private readonly lpaService: LPAService) {}

  @ApiOperation({ summary: 'Calcular ruta segura usando algoritmo LPA*' })
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        startNodeId: { type: 'number', example: 1, description: 'ID del nodo inicial' },
        goalNodeId: { type: 'number', example: 10, description: 'ID del nodo destino' },
        safetyProfile: { 
          type: 'string', 
          example: 'balanced', 
          enum: ['fastest', 'safest', 'balanced'],
          description: 'Perfil de seguridad: fastest, safest o balanced'
        }
      }
    }
  })
  @ApiResponse({ status: 201, description: 'Ruta calculada correctamente' })
  @ApiResponse({ status: 400, description: 'Error al calcular la ruta' })
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

      // CORRECCIÓN: Agregamos 'await' aquí
      const result = await this.lpaService.calculateRoute(
        startNodeId,
        goalNodeId,
        alpha,
        beta
      );

      return {
        success: true,
        data: result,
        message: 'Ruta calculada exitosamente'
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

  @ApiOperation({ summary: 'Verificar salud del servicio de routing' })
  @ApiResponse({ status: 200, description: 'Servicio funcionando correctamente' })
  @Get('health')
  health() {
    return {
      status: 'ok',
      service: 'routing',
      timestamp: new Date().toISOString(),
    };
  }
}