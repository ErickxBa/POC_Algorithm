import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // --- CONFIGURACIÓN SWAGGER ---
  const config = new DocumentBuilder()
    .setTitle('Alertify API')
    .setDescription('API de enrutamiento seguro con Algoritmo LPA* y OpenStreetMap')
    .setVersion('1.0')
    .addTag('Graph', 'Gestión del grafo y mapas')
    .addTag('Routing', 'Cálculo de rutas seguras')
    .addTag('Incidents', 'Reporte de incidentes')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);
  // -----------------------------

  app.enableCors(); // Importante para que Android pueda conectarse
  await app.listen(3000);
}
bootstrap();