import { Module } from '@nestjs/common';
import { LPAService } from './lpa.service';
import { RoutingController } from './routing.controller';
import { GraphModule } from '../graph/graph.module';

@Module({
  imports: [GraphModule],
  controllers: [RoutingController],
  providers: [LPAService],
  exports: [LPAService],
})
export class LPAModule {}
