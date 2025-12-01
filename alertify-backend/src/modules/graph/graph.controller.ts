import { Controller, Get, HttpException, HttpStatus } from '@nestjs/common';
import { GraphService } from './graph.service';

@Controller('graph')
export class GraphController {
  constructor(private readonly graphService: GraphService) {}

  @Get('status')
  getStatus() {
    try {
      return {
        success: true,
        data: this.graphService.getGraphStatus(),
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
