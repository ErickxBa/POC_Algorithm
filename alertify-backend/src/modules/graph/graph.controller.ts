import { Controller, Get, Post, Body, HttpException, HttpStatus } from '@nestjs/common';
import { GraphService } from './graph.service';

@Controller('graph')
export class GraphController {
  constructor(private readonly graphService: GraphService) {}

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

  @Get('status')
  async getStatus() {
    return {
      success: true,
      data: this.graphService.getGraphStatus(),
      nodes: await this.graphService.getNodes(),
      edges: await this.graphService.getEdges()
    };
  }

  @Get('nodes')
  async getNodes() { return { success: true, data: await this.graphService.getNodes() }; }

  @Get('edges')
  async getEdges() { return { success: true, data: await this.graphService.getEdges() }; }
}