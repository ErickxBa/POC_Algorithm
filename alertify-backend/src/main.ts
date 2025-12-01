import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { Logger } from '@nestjs/common';

/**
 * Punto de entrada de la aplicación NestJS
 */
const logger = new Logger('Bootstrap');

async function main() {
  const app = await NestFactory.create(AppModule);

  // Habilitar CORS
  app.enableCors({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
    credentials: true
  });

  // Prefijo global para API
  app.setGlobalPrefix('api/v1');

  const port = process.env.PORT || 3000;
  const env = process.env.NODE_ENV || 'development';

  await app.listen(port, () => {
    logger.log(`Alertify Backend iniciado en puerto ${port}`);
    logger.log(`Entorno: ${env}`);
    logger.log(`URL: http://localhost:${port}`);
    logger.log(`API: http://localhost:${port}/api/v1`);
  });
}

main();
