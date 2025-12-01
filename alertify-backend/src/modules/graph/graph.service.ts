import { Injectable, Logger } from '@nestjs/common';
import { GraphDatabaseService, GraphData } from '../../shared/database/graph-database.service';

@Injectable()
export class GraphService {
  private readonly logger = new Logger(GraphService.name);
  private graphData: GraphData;

  constructor(private graphDatabaseService: GraphDatabaseService) {}

  async initializeGraph() {
    try {
      this.graphData = await this.graphDatabaseService.loadGraph();
      this.logger.log('✓ Graph initialized with ' + this.graphData.nodes.length + ' nodes and ' + this.graphData.edges.length + ' edges');
    } catch (error) {
      this.logger.error('Error initializing graph', error);
      throw error;
    }
  }

  async initializeGraphAroundLocation(lat: number, lon: number) {
    return this.graphDatabaseService.initializeGraphAroundLocation(lat, lon);
  }

  getGraphStatus() {
    return {
      status: 'loaded',
      nodeCount: this.graphData?.nodes?.length || 0,
      edgeCount: this.graphData?.edges?.length || 0,
      incidentCount: this.graphData?.incidentReports?.length || 0,
      loadedAt: new Date().toISOString(),
    };
  }

  getNodes() {
    return this.graphData?.nodes || [];
  }

  getEdges() {
    return this.graphData?.edges || [];
  }

  isGraphLoaded() {
    return !!this.graphData && this.graphData.nodes && this.graphData.nodes.length > 0;
  }
}
