import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { GraphService } from './modules/graph/graph.service';

/**
 * Servicio principal de la aplicación
 */
@Injectable()
export class AppService implements OnModuleInit {
  private readonly logger = new Logger(AppService.name);

  constructor(private readonly graphService: GraphService) {}

  /**
   * Se ejecuta cuando el módulo está completamente inicializado
   */
  async onModuleInit() {
    try {
      this.logger.log('Inicializando aplicación Alertify...');
      
      // Cargar el grafo de la ciudad
      await this.graphService.initializeGraph();
      
      this.logger.log('✓ Aplicación iniciada correctamente');
    } catch (error) {
      this.logger.error('Error durante la inicialización', error);
      process.exit(1);
    }
  }

  getHello(): string {
    return '¡Bienvenido a Alertify! Sistema de ruteo dinámico con LPA*';
  }

  getStatus(): any {
    return {
      status: 'online',
      service: 'Alertify LPA* Routing Engine',
      version: '1.0.0',
      graphLoaded: this.graphService.isGraphLoaded(),
      timestamp: new Date().toISOString()
    };
  }
}
