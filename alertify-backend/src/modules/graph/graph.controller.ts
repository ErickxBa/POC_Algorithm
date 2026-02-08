import { Controller, Get, Post, Body, HttpException, HttpStatus } from '@nestjs/common';
import { ApiOperation, ApiResponse, ApiTags, ApiBody } from '@nestjs/swagger';
import { GraphService } from './graph.service';

@ApiTags('Graph')
@Controller('graph')
export class GraphController {
  constructor(private readonly graphService: GraphService) {}

  @ApiOperation({ summary: 'Inicializar el grafo alrededor de una ubicación' })
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        latitude: { type: 'number', example: -17.783 },
        longitude: { type: 'number', example: -63.182 }
      }
    }
  })
  @ApiResponse({ status: 201, description: 'Grafo inicializado correctamente' })
  @Post('initialize')
  async initializeGraph(@Body() body: { latitude: number; longitude: number }) {
    try {
      const data = await this.graphService.initializeGraphAroundLocation(body.latitude, body.longitude);
      return {
        success: true,
        message: 'Red generada en tu ubicación',
        data,
        nodes: data.nodes,
        edges: data.edges
      };
    } catch (error) {
      throw new HttpException({ success: false, message: error.message }, 500);
    }
  }

  @ApiOperation({ summary: 'Obtener estado del grafo' })
  @ApiResponse({ status: 200, description: 'Estado del grafo obtenido correctamente' })
  @Get('status')
  async getStatus() {
    return {
      success: true,
      data: this.graphService.getGraphStatus(),
      nodes: await this.graphService.getNodes(),
      edges: await this.graphService.getEdges()
    };
  }

  @ApiOperation({ summary: 'Obtener todos los nodos del grafo' })
  @ApiResponse({ status: 200, description: 'Nodos obtenidos correctamente' })
  @Get('nodes')
  async getNodes() { return { success: true, data: await this.graphService.getNodes() }; }

  @ApiOperation({ summary: 'Obtener todos los aristas del grafo' })
  @ApiResponse({ status: 200, description: 'Aristas obtenidas correctamente' })
  @Get('edges')
  async getEdges() { return { success: true, data: await this.graphService.getEdges() }; }
}