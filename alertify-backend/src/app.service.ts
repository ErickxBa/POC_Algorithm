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
      
      // ELIMINAMOS ESTA LÍNEA PORQUE AHORA EL GRAFO SE INICIALIZA DESDE EL CELULAR
      // await this.graphService.initializeGraph();

      this.logger.log('✓ Aplicación iniciada correctamente. Esperando ubicación del usuario...');
    } catch (error) {
      this.logger.error('Error durante la inicialización', error);
      // No matamos el proceso, dejamos que arranque aunque sea vacío
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
      // graphLoaded: this.graphService.isGraphLoaded(), // Opcional: Puedes descomentar si implementaste isGraphLoaded
      timestamp: new Date().toISOString()
    };
  }
}