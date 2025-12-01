import { Module } from '@nestjs/common';
import { GraphService } from './graph.service';
import { GraphController } from './graph.controller';
import { GraphDatabaseService } from '../../shared/database/graph-database.service';

@Module({
  controllers: [GraphController],
  providers: [GraphService, GraphDatabaseService],
  exports: [GraphService, GraphDatabaseService],
})
export class GraphModule {}
