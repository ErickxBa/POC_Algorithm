import { Controller, Get, Post, Body, HttpException, HttpStatus } from '@nestjs/common';
import { GraphService } from './graph.service';

@Controller('graph')
export class GraphController {
  constructor(private readonly graphService: GraphService) {}

  @Get('status')
  getStatus() {
    try {
      // Obtenemos todos los datos para que el mapa se pinte de una vez
      const statusData = this.graphService.getGraphStatus();
      const nodes = this.graphService.getNodes();
      const edges = this.graphService.getEdges();

      return {
        success: true,
        data: statusData,
        nodes: nodes,
        edges: edges,
        message: 'Grafo cargado correctamente'
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message,
        },
        HttpStatus.INTERNAL_SERVER_ERROR
      );
    }
  }

  // --- NUEVO ENDPOINT PARA INICIALIZAR EN TU UBICACIÓN ---
  @Post('initialize')
  async initializeGraph(@Body() body: { latitude: number; longitude: number }) {
    try {
      const { latitude, longitude } = body;

      // Llamamos al servicio para crear nodos alrededor de esa coordenada
      const data = await this.graphService.initializeGraphAroundLocation(latitude, longitude);

      return {
        success: true,
        message: 'Grafo generado alrededor de tu ubicación',
        data,
        nodes: data.nodes,
        edges: data.edges
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message
        },
        HttpStatus.INTERNAL_SERVER_ERROR
      );
    }
  }
  // ------------------------------------------------------

  @Get('nodes')
  getNodes() {
    try {
      return {
        success: true,
        data: this.graphService.getNodes(),
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message,
        },
        HttpStatus.INTERNAL_SERVER_ERROR
      );
    }
  }

  @Get('edges')
  getEdges() {
    try {
      return {
        success: true,
        data: this.graphService.getEdges(),
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message,
        },
        HttpStatus.INTERNAL_SERVER_ERROR
      );
    }
  }

  @Get('health')
  health() {
    return {
      status: 'ok',
      service: 'graph',
      timestamp: new Date().toISOString(),
    };
  }
}