import { Injectable, Logger } from '@nestjs/common';
import { GraphDatabaseService } from '../../shared/database/graph-database.service';

@Injectable()
export class GraphService {
  private readonly logger = new Logger(GraphService.name);

  constructor(private graphDatabaseService: GraphDatabaseService) {}

  async initializeGraphAroundLocation(lat: number, lon: number) {
    this.logger.log(`Inicializando grafo en: ${lat}, ${lon}`);
    return this.graphDatabaseService.initializeGraphAroundLocation(lat, lon);
  }

  getGraphStatus() { return this.graphDatabaseService.getGraphStatus(); }
  getNodes() { return this.graphDatabaseService.getNodes(); }
  getEdges() { return this.graphDatabaseService.getEdges(); }
  isGraphLoaded() { return true; }
}