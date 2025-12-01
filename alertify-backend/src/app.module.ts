import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { LPAModule } from './modules/lpa/lpa.module';
import { IncidentsModule } from './modules/incidents/incidents.module';
import { GraphModule } from './modules/graph/graph.module';
import { AppController } from './app.controller';
import { AppService } from './app.service';

/**
 * Módulo principal de la aplicación
 * Integra:
 * - Módulo LPA* para ruteo inteligente
 * - Módulo de Incidentes para reportes en tiempo real
 * - Módulo Graph para gestión del grafo de la ciudad
 */
@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env'
    }),
    GraphModule,
    LPAModule,
    IncidentsModule
  ],
  controllers: [AppController],
  providers: [AppService]
})
export class AppModule {}
